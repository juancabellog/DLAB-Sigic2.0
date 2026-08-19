package com.sisgic.controller;

import com.sisgic.dto.FundingTypeDTO;
import com.sisgic.dto.ParticipanteDTO;
import com.sisgic.dto.ProjectProductDTO;
import com.sisgic.dto.TipoProductoDTO;
import com.sisgic.entity.*;
import com.sisgic.repository.*;
import com.sisgic.service.TextosService;
import com.sisgic.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/projects")
@CrossOrigin(origins = "*")
public class ProyectoCientificoController {

    private static final Logger log = LoggerFactory.getLogger(ProyectoCientificoController.class);
    /** Fundingtype id for "Other" → requires otherFundingType. */
    private static final long FUNDING_OTHER_ID = 7L;
    /** tipoproyecto id for "Other" → requires otherProjectType. */
    private static final long PROJECT_TYPE_OTHER_ID = 4L;
    /** Fixed catalog id in v_tipo_producto / tipo producto. */
    private static final long PROJECT_TIPO_PRODUCTO_ID = 19L;

    @Autowired
    private ProyectoCientificoRepository proyectoCientificoRepository;

    @Autowired
    private FundingTypeRepository fundingTypeRepository;

    @Autowired
    private TipoProyectoRepository tipoProyectoRepository;

    @Autowired
    private TipoProductoRepository tipoProductoRepository;

    @Autowired
    private RRHHRepository rrhhRepository;

    @Autowired
    private TipoParticipacionRepository tipoParticipacionRepository;

    @Autowired
    private ParticipacionProductoRepository participacionProductoRepository;

    @Autowired
    private TextosService textosService;

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<Page<ProjectProductDTO>> getProjects(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
            ? Sort.by(sortBy).descending()
            : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Long idRRHH = userService.getCurrentUserIdRRHH().orElse(null);
        String userName = userService.getCurrentUsername().orElse(null);
        Page<ProyectoCientifico> projects = proyectoCientificoRepository.findVisibleByUserIdRRHH(idRRHH, userName, pageable);

        List<ProyectoCientifico> content = projects.getContent();
        List<String> codigosTexto = new ArrayList<>();
        for (ProyectoCientifico p : content) {
            if (p.getDescripcion() != null && !p.getDescripcion().isEmpty()) {
                codigosTexto.add(p.getDescripcion());
            }
            if (p.getComentario() != null && !p.getComentario().isEmpty()) {
                codigosTexto.add(p.getComentario());
            }
        }
        Map<String, String> textosMap = textosService.getTextValuesBatch(codigosTexto, 2, "us");
        Map<Long, String> projectTypeLabels = loadProjectTypeLabels();

        return ResponseEntity.ok(projects.map(p -> convertToDTOWithoutParticipants(p, textosMap, projectTypeLabels)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectProductDTO> getProject(@PathVariable Long id) {
        Long idRRHH = userService.getCurrentUserIdRRHH().orElse(null);
        String userName = userService.getCurrentUsername().orElse(null);
        return proyectoCientificoRepository.findVisibleByIdAndUserIdRRHH(id, idRRHH, userName)
            .map(entity -> ResponseEntity.ok(convertToDTO(entity, loadProjectTypeLabels())))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> createProject(@RequestBody ProjectProductDTO dto) {
        try {
            validateDto(dto, null);
            ProyectoCientifico entity = convertFromDTO(dto);
            userService.getCurrentUsername().ifPresent(entity::setUsername);

            TipoProducto tipoProducto = resolveProjectProductType();
            entity.setTipoProducto(tipoProducto);

            ProyectoCientifico saved = proyectoCientificoRepository.save(entity);
            if (dto.getParticipantes() != null && !dto.getParticipantes().isEmpty()) {
                saveParticipantes(saved, dto.getParticipantes());
            }
            return ResponseEntity.ok(convertToDTO(saved, loadProjectTypeLabels()));
        } catch (IllegalArgumentException ex) {
            log.warn("Create project validation failed: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (Exception e) {
            log.error("Error creating project", e);
            return ResponseEntity.badRequest().body(Map.of("message", "Could not create project"));
        }
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> updateProject(@PathVariable Long id, @RequestBody ProjectProductDTO dto) {
        try {
            validateDto(dto, id);
            return proyectoCientificoRepository.findById(id)
                .map(existing -> {
                    applyProductFields(existing, dto, true);
                    applyProjectFields(existing, dto);
                    existing.setTipoProducto(resolveProjectProductType());
                    if (dto.getParticipantes() != null) {
                        participacionProductoRepository.deleteByProductoId(existing.getId());
                        if (!dto.getParticipantes().isEmpty()) {
                            saveParticipantes(existing, dto.getParticipantes());
                        }
                    }
                    ProyectoCientifico updated = proyectoCientificoRepository.save(existing);
                    return ResponseEntity.ok(convertToDTO(updated, loadProjectTypeLabels()));
                })
                .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException ex) {
            log.warn("Update project validation failed: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (Exception e) {
            log.error("Error updating project id={}", id, e);
            return ResponseEntity.badRequest().body(Map.of("message", "Could not update project"));
        }
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        return proyectoCientificoRepository.findById(id)
            .map(entity -> {
                participacionProductoRepository.deleteByProductoId(id);
                proyectoCientificoRepository.deleteById(id);
                return ResponseEntity.noContent().<Void>build();
            })
            .orElse(ResponseEntity.notFound().build());
    }

    private void validateDto(ProjectProductDTO dto, Long excludeId) {
        if (dto.getDescripcion() == null || dto.getDescripcion().isBlank()) {
            throw new IllegalArgumentException("Project Title is required");
        }
        if (dto.getProjectCode() == null || dto.getProjectCode().isBlank()) {
            throw new IllegalArgumentException("Project Code is required");
        }
        String code = dto.getProjectCode().trim();
        if (code.length() > 100) {
            throw new IllegalArgumentException("Project Code max length is 100");
        }
        boolean codeExists = excludeId == null
            ? proyectoCientificoRepository.existsByProjectCodeIgnoreCase(code)
            : proyectoCientificoRepository.existsByProjectCodeIgnoreCaseAndIdNot(code, excludeId);
        if (codeExists) {
            throw new IllegalArgumentException("Project Code already exists");
        }
        if (dto.getAwardDate() == null || dto.getAwardDate().isBlank()) {
            throw new IllegalArgumentException("Award Date is required");
        }
        if (dto.getDuration() == null || dto.getDuration() <= 0) {
            throw new IllegalArgumentException("Duration must be a positive integer");
        }
        if (dto.getTotalAmount() == null || dto.getTotalAmount() < 0) {
            throw new IllegalArgumentException("Total amount must be a non-negative integer");
        }
        if (dto.getTotalAmountCenter() == null || dto.getTotalAmountCenter() < 0) {
            throw new IllegalArgumentException("Center amount must be a non-negative integer");
        }
        if (dto.getFechaInicio() == null || dto.getFechaInicio().isBlank()) {
            throw new IllegalArgumentException("Start Date is required");
        }
        LocalDate start = parseLocalDate(dto.getFechaInicio());
        LocalDate end = parseLocalDate(dto.getFechaTermino());
        if (start != null && end != null && end.isBefore(start)) {
            throw new IllegalArgumentException("Ending Date must be equal to or after Start Date");
        }
        if (dto.getProjectTypes() == null || dto.getProjectTypes().isBlank()) {
            throw new IllegalArgumentException("Project Type is required");
        }
        boolean hasOtherType = projectTypesContainsId(dto.getProjectTypes(), PROJECT_TYPE_OTHER_ID);
        if (hasOtherType && (dto.getOtherProjectType() == null || dto.getOtherProjectType().isBlank())) {
            throw new IllegalArgumentException("Other Project Type is required when Other is selected");
        }

        Long fundingId = resolveFundingTypeId(dto);
        fundingTypeRepository.findById(fundingId)
            .orElseThrow(() -> new IllegalArgumentException("Funding Source is required"));
        boolean isOtherFunding = fundingId != null && fundingId.equals(FUNDING_OTHER_ID);
        if (isOtherFunding && (dto.getOtherFundingType() == null || dto.getOtherFundingType().isBlank())) {
            throw new IllegalArgumentException("Other Funding Source is required when Other is selected");
        }
    }

    private Long resolveFundingTypeId(ProjectProductDTO dto) {
        if (dto.getIdFundingtype() != null) {
            return dto.getIdFundingtype();
        }
        if (dto.getFundingType() != null && dto.getFundingType().getId() != null) {
            return dto.getFundingType().getId();
        }
        throw new IllegalArgumentException("Funding Source is required");
    }

    private TipoProducto resolveProjectProductType() {
        return tipoProductoRepository.findById(PROJECT_TIPO_PRODUCTO_ID)
            .orElseThrow(() -> new IllegalStateException(
                "TipoProducto with id=" + PROJECT_TIPO_PRODUCTO_ID + " (PROYECTO) not found"));
    }

    private ProyectoCientifico convertFromDTO(ProjectProductDTO dto) {
        ProyectoCientifico entity = new ProyectoCientifico();
        applyProductFields(entity, dto, false);
        applyProjectFields(entity, dto);
        return entity;
    }

    private void applyProductFields(ProyectoCientifico entity, ProjectProductDTO dto, boolean isUpdate) {
        if (dto.getDescripcion() != null && !dto.getDescripcion().isBlank()) {
            if (isUpdate && entity.getDescripcion() != null && !entity.getDescripcion().isEmpty()) {
                textosService.updateTextInBothLanguages(entity.getDescripcion(), dto.getDescripcion().trim(), 2);
            } else {
                entity.setDescripcion(textosService.createTextInBothLanguages(dto.getDescripcion().trim(), 2));
            }
        }
        if (dto.getComentario() != null && !dto.getComentario().isBlank()) {
            if (isUpdate && entity.getComentario() != null && !entity.getComentario().isEmpty()) {
                textosService.updateTextInBothLanguages(entity.getComentario(), dto.getComentario().trim(), 2);
            } else {
                entity.setComentario(textosService.createTextInBothLanguages(dto.getComentario().trim(), 2));
            }
        } else if (isUpdate) {
            // leave existing or clear? keep if blank sent after existing - set null only if intentionally empty and had no code
            if (dto.getComentario() != null && dto.getComentario().isBlank()) {
                entity.setComentario(null);
            }
        }
        entity.setFechaInicio(parseLocalDate(dto.getFechaInicio()));
        entity.setFechaTermino(parseLocalDate(dto.getFechaTermino()));
        entity.setProgressReport(dto.getProgressReport());
        entity.setCodigoANID(dto.getCodigoANID());
        if (dto.getBasal() != null && !dto.getBasal().isEmpty()) {
            char basalValue = Character.toUpperCase(dto.getBasal().charAt(0));
            entity.setBasal((basalValue == 'S' || basalValue == 'N') ? basalValue : 'N');
        } else {
            entity.setBasal(isUpdate ? 'N' : 'S');
        }
        entity.setCluster(dto.getCluster());
    }

    private void applyProjectFields(ProyectoCientifico entity, ProjectProductDTO dto) {
        entity.setProjectCode(dto.getProjectCode().trim());
        entity.setAwardDate(parseLocalDate(dto.getAwardDate()));
        entity.setDuration(dto.getDuration());
        entity.setTotalAmount(dto.getTotalAmount());
        entity.setTotalAmountCenter(dto.getTotalAmountCenter());

        Long fundingId = resolveFundingTypeId(dto);
        FundingType funding = fundingTypeRepository.findById(fundingId)
            .orElseThrow(() -> new IllegalArgumentException("Funding Source is required"));
        entity.setFundingType(funding);

        boolean isOtherFunding = fundingId != null && fundingId.equals(FUNDING_OTHER_ID);
        entity.setOtherFundingType(isOtherFunding ? trimToNull(dto.getOtherFundingType()) : null);

        String projectTypes = normalizeProjectTypes(dto.getProjectTypes());
        entity.setProjectTypes(projectTypes);

        boolean hasOtherType = projectTypesContainsId(projectTypes, PROJECT_TYPE_OTHER_ID);
        entity.setOtherProjectType(hasOtherType ? trimToNull(dto.getOtherProjectType()) : null);

        entity.setNameSocialOrganizations(trimToNull(dto.getNameSocialOrganizations()));
        entity.setNamePublicSectorEntities(trimToNull(dto.getNamePublicSectorEntities()));
        entity.setNamePrivateSectorEntities(trimToNull(dto.getNamePrivateSectorEntities()));
        entity.setNameTradeRegionalAssociations(trimToNull(dto.getNameTradeRegionalAssociations()));
        entity.setNameSTEntities(trimToNull(dto.getNameSTEntities()));
    }

    private boolean projectTypesContainsId(String projectTypes, long id) {
        if (projectTypes == null || projectTypes.isBlank()) {
            return false;
        }
        String target = String.valueOf(id);
        return Arrays.stream(projectTypes.split(","))
            .map(String::trim)
            .anyMatch(target::equals);
    }

    private String normalizeProjectTypes(String raw) {
        return Arrays.stream(raw.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .filter(s -> {
                try {
                    Long.parseLong(s);
                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }
            })
            .distinct()
            .collect(Collectors.joining(","));
    }

    private ProjectProductDTO convertToDTOWithoutParticipants(
            ProyectoCientifico entity,
            Map<String, String> textosMap,
            Map<Long, String> projectTypeLabels) {
        ProjectProductDTO dto = convertToDTOBase(entity, textosMap, projectTypeLabels);
        dto.setParticipantes(null);
        return dto;
    }

    private ProjectProductDTO convertToDTO(ProyectoCientifico entity, Map<Long, String> projectTypeLabels) {
        ProjectProductDTO dto = convertToDTOBase(entity, null, projectTypeLabels);
        List<ParticipacionProducto> participaciones = participacionProductoRepository.findByProductoId(entity.getId());
        List<ParticipanteDTO> participantesDTO = participaciones.stream()
            .map(pp -> {
                ParticipanteDTO pDTO = new ParticipanteDTO();
                pDTO.setRrhhId(pp.getRrhh() != null ? pp.getRrhh().getId() : null);
                pDTO.setTipoParticipacionId(pp.getTipoParticipacion() != null ? pp.getTipoParticipacion().getId() : null);
                pDTO.setOrden(pp.getOrden());
                pDTO.setCorresponding(pp.isCorresponding());
                return pDTO;
            })
            .collect(Collectors.toList());
        dto.setParticipantes(participantesDTO);
        return dto;
    }

    private ProjectProductDTO convertToDTOBase(
            ProyectoCientifico entity,
            Map<String, String> textosMap,
            Map<Long, String> projectTypeLabels) {
        ProjectProductDTO dto = new ProjectProductDTO();
        dto.setId(entity.getId());

        if (entity.getDescripcion() != null && !entity.getDescripcion().isEmpty()) {
            String descripcion = textosMap != null && textosMap.containsKey(entity.getDescripcion())
                ? textosMap.get(entity.getDescripcion())
                : textosService.getTextValue(entity.getDescripcion(), 2, "us").orElse(entity.getDescripcion());
            dto.setDescripcion(descripcion);
        }
        if (entity.getComentario() != null && !entity.getComentario().isEmpty()) {
            String comentario = textosMap != null && textosMap.containsKey(entity.getComentario())
                ? textosMap.get(entity.getComentario())
                : textosService.getTextValue(entity.getComentario(), 2, "us").orElse(entity.getComentario());
            dto.setComentario(comentario);
        }
        dto.setFechaInicio(entity.getFechaInicio() != null ? entity.getFechaInicio().toString() : null);
        dto.setFechaTermino(entity.getFechaTermino() != null ? entity.getFechaTermino().toString() : null);
        dto.setProgressReport(entity.getProgressReport());
        dto.setCodigoANID(entity.getCodigoANID());
        if (entity.getBasal() != null) {
            char basalChar = entity.getBasal();
            if (basalChar == '1') {
                dto.setBasal("S");
            } else if (basalChar == '0') {
                dto.setBasal("N");
            } else {
                dto.setBasal(String.valueOf(basalChar));
            }
        }
        dto.setCluster(entity.getCluster());
        dto.setParticipantesNombres(entity.getParticipantesNombres());
        dto.setMainResponsible(entity.getMainResponsible());
        dto.setCreatedAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null);
        dto.setUpdatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().toString() : null);

        if (entity.getTipoProducto() != null) {
            Long tipoId = entity.getTipoProducto().getId();
            TipoProducto tipoProducto = tipoId != null
                ? tipoProductoRepository.findById(tipoId).orElse(null)
                : null;
            TipoProductoDTO tipoDTO = new TipoProductoDTO();
            tipoDTO.setId(tipoId);
            if (tipoProducto != null) {
                tipoDTO.setIdDescripcion(tipoProducto.getIdDescripcion());
                tipoDTO.setDescripcion(tipoProducto.getDescripcion());
            }
            dto.setTipoProducto(tipoDTO);
        }

        dto.setProjectCode(entity.getProjectCode());
        dto.setAwardDate(entity.getAwardDate() != null ? entity.getAwardDate().toString() : null);
        dto.setDuration(entity.getDuration());
        dto.setTotalAmount(entity.getTotalAmount());
        dto.setTotalAmountCenter(entity.getTotalAmountCenter());
        dto.setProjectTypes(entity.getProjectTypes());
        dto.setOtherProjectType(entity.getOtherProjectType());
        dto.setOtherFundingType(entity.getOtherFundingType());
        dto.setNameSocialOrganizations(entity.getNameSocialOrganizations());
        dto.setNamePublicSectorEntities(entity.getNamePublicSectorEntities());
        dto.setNamePrivateSectorEntities(entity.getNamePrivateSectorEntities());
        dto.setNameTradeRegionalAssociations(entity.getNameTradeRegionalAssociations());
        dto.setNameSTEntities(entity.getNameSTEntities());

        if (entity.getFundingType() != null) {
            Long fundingId = entity.getFundingType().getId();
            // Avoid LazyInitializationException outside Session (list native queries, detached entities).
            FundingType funding = fundingId != null
                ? fundingTypeRepository.findById(fundingId).orElse(null)
                : null;
            FundingTypeDTO fundingDTO = new FundingTypeDTO();
            fundingDTO.setId(fundingId);
            if (funding != null) {
                fundingDTO.setIdDescripcion(funding.getIdDescripcion());
                dto.setFundingTypeLabel(funding.getIdDescripcion());
            }
            dto.setFundingType(fundingDTO);
            dto.setIdFundingtype(fundingId);
        }

        dto.setProjectTypesLabels(resolveProjectTypesLabels(entity.getProjectTypes(), projectTypeLabels, entity.getOtherProjectType()));
        return dto;
    }

    private String resolveProjectTypesLabels(String projectTypes, Map<Long, String> labels, String otherText) {
        if (projectTypes == null || projectTypes.isBlank()) {
            return "";
        }
        return Arrays.stream(projectTypes.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(token -> {
                try {
                    Long id = Long.parseLong(token);
                    if (id == PROJECT_TYPE_OTHER_ID && otherText != null && !otherText.isBlank()) {
                        return "Other (" + otherText + ")";
                    }
                    return labels.getOrDefault(id, token);
                } catch (NumberFormatException e) {
                    return token;
                }
            })
            .collect(Collectors.joining(", "));
    }

    private Map<Long, String> loadProjectTypeLabels() {
        return tipoProyectoRepository.findAll().stream()
            .filter(t -> t.getId() != null)
            .collect(Collectors.toMap(TipoProyecto::getId, t -> t.getIdDescripcion() != null ? t.getIdDescripcion() : "", (a, b) -> a));
    }

    private void saveParticipantes(ProyectoCientifico project, List<ParticipanteDTO> participantesDTO) {
        for (ParticipanteDTO pDTO : participantesDTO) {
            if (pDTO.getRrhhId() == null || pDTO.getTipoParticipacionId() == null) {
                continue;
            }
            RRHH rrhh = rrhhRepository.findById(pDTO.getRrhhId()).orElse(null);
            TipoParticipacion tipoParticipacion = tipoParticipacionRepository.findById(pDTO.getTipoParticipacionId()).orElse(null);
            if (rrhh == null || tipoParticipacion == null) {
                continue;
            }
            Long nextId = participacionProductoRepository.getNextIdForParticipacion(project.getId(), rrhh.getId());
            ParticipacionProducto participacion = new ParticipacionProducto();
            participacion.setRrhh(rrhh);
            participacion.setProducto(project);
            participacion.setTipoParticipacion(tipoParticipacion);
            participacion.setOrden(pDTO.getOrden() != null ? pDTO.getOrden() : 0);
            participacion.setCorresponding(pDTO.getCorresponding() != null && pDTO.getCorresponding());
            participacion.setId(new ParticipacionProductoId(rrhh.getId(), project.getId(), nextId));
            participacionProductoRepository.save(participacion);
        }
    }

    private LocalDate parseLocalDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr.trim());
        } catch (Exception e) {
            try {
                return LocalDate.parse(dateStr.trim(), DateTimeFormatter.ISO_DATE);
            } catch (Exception e2) {
                throw new IllegalArgumentException("Invalid date: " + dateStr);
            }
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
