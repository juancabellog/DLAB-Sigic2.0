package com.sisgic.controller;

import com.sisgic.dto.AwardProductDTO;
import com.sisgic.dto.InstitucionDTO;
import com.sisgic.dto.ParticipanteDTO;
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
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/awards")
@CrossOrigin(origins = "*")
public class AwardController {

    private static final Logger log = LoggerFactory.getLogger(AwardController.class);
    private static final long AWARD_TIPO_PRODUCTO_ID = 21L;

    @Autowired private AwardRepository awardRepository;
    @Autowired private InstitucionRepository institucionRepository;
    @Autowired private PaisRepository paisRepository;
    @Autowired private TipoProductoRepository tipoProductoRepository;
    @Autowired private RRHHRepository rrhhRepository;
    @Autowired private TipoParticipacionRepository tipoParticipacionRepository;
    @Autowired private ParticipacionProductoRepository participacionProductoRepository;
    @Autowired private TextosService textosService;
    @Autowired private UserService userService;

    @GetMapping
    public ResponseEntity<Page<AwardProductDTO>> getAwards(
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
        Page<Award> awards = awardRepository.findVisibleByUserIdRRHH(idRRHH, userName, pageable);

        List<String> codigosTexto = new ArrayList<>();
        for (Award a : awards.getContent()) {
            if (a.getDescripcion() != null && !a.getDescripcion().isEmpty()) {
                codigosTexto.add(a.getDescripcion());
            }
            if (a.getComentario() != null && !a.getComentario().isEmpty()) {
                codigosTexto.add(a.getComentario());
            }
        }
        Map<String, String> textosMap = textosService.getTextValuesBatch(codigosTexto, 2, "us");
        return ResponseEntity.ok(awards.map(a -> convertToDTOWithoutParticipants(a, textosMap)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AwardProductDTO> getAward(@PathVariable Long id) {
        Long idRRHH = userService.getCurrentUserIdRRHH().orElse(null);
        String userName = userService.getCurrentUsername().orElse(null);
        return awardRepository.findVisibleByIdAndUserIdRRHH(id, idRRHH, userName)
            .map(entity -> ResponseEntity.ok(convertToDTO(entity)))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> createAward(@RequestBody AwardProductDTO dto) {
        try {
            validateDto(dto);
            Award entity = convertFromDTO(dto);
            userService.getCurrentUsername().ifPresent(entity::setUsername);
            entity.setTipoProducto(resolveAwardProductType());
            Award saved = awardRepository.save(entity);
            if (dto.getParticipantes() != null && !dto.getParticipantes().isEmpty()) {
                saveParticipantes(saved, dto.getParticipantes());
            }
            return ResponseEntity.ok(convertToDTO(saved));
        } catch (IllegalArgumentException ex) {
            log.warn("Create award validation failed: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (Exception e) {
            log.error("Error creating award", e);
            return ResponseEntity.badRequest().body(Map.of("message", "Could not create award"));
        }
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> updateAward(@PathVariable Long id, @RequestBody AwardProductDTO dto) {
        try {
            validateDto(dto);
            return awardRepository.findById(id)
                .map(existing -> {
                    applyProductFields(existing, dto, true);
                    applyAwardFields(existing, dto);
                    existing.setTipoProducto(resolveAwardProductType());
                    if (dto.getParticipantes() != null) {
                        participacionProductoRepository.deleteByProductoId(existing.getId());
                        if (!dto.getParticipantes().isEmpty()) {
                            saveParticipantes(existing, dto.getParticipantes());
                        }
                    }
                    Award updated = awardRepository.save(existing);
                    return ResponseEntity.ok(convertToDTO(updated));
                })
                .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException ex) {
            log.warn("Update award validation failed: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (Exception e) {
            log.error("Error updating award id={}", id, e);
            return ResponseEntity.badRequest().body(Map.of("message", "Could not update award"));
        }
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deleteAward(@PathVariable Long id) {
        return awardRepository.findById(id)
            .map(entity -> {
                participacionProductoRepository.deleteByProductoId(id);
                awardRepository.deleteById(id);
                return ResponseEntity.noContent().<Void>build();
            })
            .orElse(ResponseEntity.notFound().build());
    }

    private void validateDto(AwardProductDTO dto) {
        if (dto.getDescripcion() == null || dto.getDescripcion().isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        int maxYear = Year.now().getValue() + 1;
        if (dto.getYear() == null || dto.getYear() < 1900 || dto.getYear() > maxYear) {
            throw new IllegalArgumentException("Year must be a valid year between 1900 and " + maxYear);
        }
        Long institutionId = resolveInstitutionId(dto);
        institucionRepository.findById(institutionId)
            .orElseThrow(() -> new IllegalArgumentException("Institution is required"));
    }

    private Long resolveInstitutionId(AwardProductDTO dto) {
        if (dto.getIdInstitucion() != null) {
            return dto.getIdInstitucion();
        }
        if (dto.getInstitucion() != null && dto.getInstitucion().getId() != null) {
            return dto.getInstitucion().getId();
        }
        throw new IllegalArgumentException("Institution is required");
    }

    private TipoProducto resolveAwardProductType() {
        return tipoProductoRepository.findById(AWARD_TIPO_PRODUCTO_ID)
            .orElseThrow(() -> new IllegalStateException(
                "TipoProducto with id=" + AWARD_TIPO_PRODUCTO_ID + " (AWARD) not found"));
    }

    private Award convertFromDTO(AwardProductDTO dto) {
        Award entity = new Award();
        applyProductFields(entity, dto, false);
        applyAwardFields(entity, dto);
        return entity;
    }

    private void applyProductFields(Award entity, AwardProductDTO dto, boolean isUpdate) {
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
        } else if (isUpdate && dto.getComentario() != null && dto.getComentario().isBlank()) {
            entity.setComentario(null);
        }

        // Year drives product start date: YYYY-01-01
        if (dto.getYear() != null) {
            entity.setFechaInicio(LocalDate.of(dto.getYear(), 1, 1));
        } else if (dto.getFechaInicio() != null && !dto.getFechaInicio().isBlank()) {
            entity.setFechaInicio(LocalDate.parse(dto.getFechaInicio().trim()));
        } else {
            entity.setFechaInicio(null);
        }
        entity.setFechaTermino(null);
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

    private void applyAwardFields(Award entity, AwardProductDTO dto) {
        entity.setYear(dto.getYear());
        Long institutionId = resolveInstitutionId(dto);
        Institucion institucion = institucionRepository.findById(institutionId)
            .orElseThrow(() -> new IllegalArgumentException("Institution is required"));
        entity.setInstitucion(institucion);
    }

    private AwardProductDTO convertToDTOWithoutParticipants(Award entity, Map<String, String> textosMap) {
        AwardProductDTO dto = convertToDTOBase(entity, textosMap);
        dto.setParticipantes(null);
        return dto;
    }

    private AwardProductDTO convertToDTO(Award entity) {
        AwardProductDTO dto = convertToDTOBase(entity, null);
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

    private AwardProductDTO convertToDTOBase(Award entity, Map<String, String> textosMap) {
        AwardProductDTO dto = new AwardProductDTO();
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

        dto.setYear(entity.getYear());

        if (entity.getInstitucion() != null) {
            Long instId = entity.getInstitucion().getId();
            Institucion institucion = instId != null
                ? institucionRepository.findById(instId).orElse(null)
                : null;
            if (institucion != null) {
                InstitucionDTO institucionDTO = new InstitucionDTO();
                institucionDTO.setId(institucion.getId());
                institucionDTO.setIdDescripcion(institucion.getIdDescripcion());
                institucionDTO.setDescripcion(institucion.getDescripcion());
                institucionDTO.setCodigoPais(institucion.getCodigoPais());
                String label = institucion.getDescripcion() != null && !institucion.getDescripcion().isBlank()
                    ? institucion.getDescripcion()
                    : institucion.getIdDescripcion();
                dto.setInstitutionLabel(label);
                dto.setIdInstitucion(institucion.getId());
                dto.setCodigoPais(institucion.getCodigoPais());
                if (institucion.getCodigoPais() != null && !institucion.getCodigoPais().isBlank()) {
                    paisRepository.findById(institucion.getCodigoPais().trim()).ifPresent(pais -> {
                        dto.setCountryLabel(pais.getIdDescripcion());
                        institucionDTO.setCountryLabel(pais.getIdDescripcion());
                    });
                }
                dto.setInstitucion(institucionDTO);
            }
        }

        return dto;
    }

    private void saveParticipantes(Award award, List<ParticipanteDTO> participantesDTO) {
        for (ParticipanteDTO pDTO : participantesDTO) {
            if (pDTO.getRrhhId() == null || pDTO.getTipoParticipacionId() == null) {
                continue;
            }
            RRHH rrhh = rrhhRepository.findById(pDTO.getRrhhId()).orElse(null);
            TipoParticipacion tipoParticipacion = tipoParticipacionRepository.findById(pDTO.getTipoParticipacionId()).orElse(null);
            if (rrhh == null || tipoParticipacion == null) {
                continue;
            }
            Long nextId = participacionProductoRepository.getNextIdForParticipacion(award.getId(), rrhh.getId());
            ParticipacionProducto participacion = new ParticipacionProducto();
            participacion.setRrhh(rrhh);
            participacion.setProducto(award);
            participacion.setTipoParticipacion(tipoParticipacion);
            participacion.setOrden(pDTO.getOrden() != null ? pDTO.getOrden() : 0);
            participacion.setCorresponding(pDTO.getCorresponding() != null && pDTO.getCorresponding());
            participacion.setId(new ParticipacionProductoId(rrhh.getId(), award.getId(), nextId));
            participacionProductoRepository.save(participacion);
        }
    }
}
