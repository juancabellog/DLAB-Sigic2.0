package com.sisgic.controller;

import com.sisgic.dto.EstadoProductoDTO;
import com.sisgic.dto.ParticipacionEventoCientificoDTO;
import com.sisgic.dto.PaisDTO;
import com.sisgic.dto.ParticipanteDTO;
import com.sisgic.dto.TipoParticipacionEventoDTO;
import com.sisgic.entity.ParticipacionEventoCientifico;
import com.sisgic.entity.ParticipacionProducto;
import com.sisgic.entity.ParticipacionProductoId;
import com.sisgic.entity.RRHH;
import com.sisgic.entity.TipoParticipacion;
import com.sisgic.entity.TipoProducto;
import com.sisgic.entity.ModalidadPresentacion;
import com.sisgic.repository.EstadoProductoRepository;
import com.sisgic.repository.PaisRepository;
import com.sisgic.repository.ParticipacionEventoCientificoRepository;
import com.sisgic.repository.ParticipacionProductoRepository;
import com.sisgic.repository.RRHHRepository;
import com.sisgic.repository.TipoParticipacionEventoRepository;
import com.sisgic.repository.TipoParticipacionRepository;
import com.sisgic.repository.TipoProductoRepository;
import com.sisgic.repository.ModalidadPresentacionRepository;
import com.sisgic.service.TextosService;
import com.sisgic.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import jakarta.servlet.http.HttpServletResponse;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/participation-scientific-events")
@CrossOrigin(origins = "*")
public class ParticipacionEventoCientificoController {

    @Autowired
    private ParticipacionEventoCientificoRepository repository;

    @Autowired
    private TipoProductoRepository tipoProductoRepository;

    @Autowired
    private EstadoProductoRepository estadoProductoRepository;

    @Autowired
    private TipoParticipacionEventoRepository tipoParticipacionEventoRepository;

    @Autowired
    private ModalidadPresentacionRepository modalidadPresentacionRepository;

    @Autowired
    private PaisRepository paisRepository;

    @Autowired
    private RRHHRepository rrhhRepository;

    @Autowired
    private TipoParticipacionRepository tipoParticipacionRepository;

    @Autowired
    private ParticipacionProductoRepository participacionProductoRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private TextosService textosService;

    @Autowired
    private com.sisgic.service.PdfFileService pdfFileService;

    @GetMapping
    public ResponseEntity<Page<ParticipacionEventoCientificoDTO>> getParticipationScientificEvents(
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
        Page<ParticipacionEventoCientifico> pageData = repository.findVisibleByUserIdRRHH(idRRHH, userName, pageable);

        List<String> textCodes = new ArrayList<>();
        for (ParticipacionEventoCientifico item : pageData.getContent()) {
            if (item.getDescripcion() != null && !item.getDescripcion().isEmpty()) {
                textCodes.add(item.getDescripcion());
            }
            if (item.getComentario() != null && !item.getComentario().isEmpty()) {
                textCodes.add(item.getComentario());
            }
            if (item.getPais() != null && item.getPais().getIdDescripcion() != null && !item.getPais().getIdDescripcion().isEmpty()) {
                textCodes.add(item.getPais().getIdDescripcion());
            }
            if (item.getTipoParticipacionEvento() != null
                    && item.getTipoParticipacionEvento().getIdDescripcion() != null
                    && !item.getTipoParticipacionEvento().getIdDescripcion().isEmpty()) {
                textCodes.add(item.getTipoParticipacionEvento().getIdDescripcion());
            }
        }
        Map<String, String> textosMap = textosService.getTextValuesBatch(textCodes, 2, "us");

        Page<ParticipacionEventoCientificoDTO> dtoPage = pageData.map(item -> toDto(item, textosMap, false));
        return ResponseEntity.ok(dtoPage);
    }

    /**
     * Exporta las participaciones en eventos científicos visibles a Excel.
     */
    @GetMapping("/export")
    @Transactional(readOnly = true)
    public void exportParticipationScientificEventsToExcel(
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            HttpServletResponse response) {

        try {
            Sort sort = sortDir.equalsIgnoreCase("desc")
                    ? Sort.by(sortBy).descending()
                    : Sort.by(sortBy).ascending();

            Long idRRHH = userService.getCurrentUserIdRRHH().orElse(null);
            String userName = userService.getCurrentUsername().orElse(null);
            List<ParticipacionEventoCientifico> items = repository
                    .findVisibleByUserIdRRHH(idRRHH, userName, Pageable.unpaged(sort))
                    .getContent();

            List<String> textCodes = new ArrayList<>();
            for (ParticipacionEventoCientifico item : items) {
                if (item.getDescripcion() != null && !item.getDescripcion().isEmpty()) {
                    textCodes.add(item.getDescripcion());
                }
                if (item.getComentario() != null && !item.getComentario().isEmpty()) {
                    textCodes.add(item.getComentario());
                }
                if (item.getPais() != null && item.getPais().getIdDescripcion() != null
                        && !item.getPais().getIdDescripcion().isEmpty()) {
                    textCodes.add(item.getPais().getIdDescripcion());
                }
                if (item.getTipoParticipacionEvento() != null
                        && item.getTipoParticipacionEvento().getIdDescripcion() != null
                        && !item.getTipoParticipacionEvento().getIdDescripcion().isEmpty()) {
                    textCodes.add(item.getTipoParticipacionEvento().getIdDescripcion());
                }
            }
            Map<String, String> textosMap = textosService.getTextValuesBatch(textCodes, 2, "us");

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Participation Events");

            int rowIdx = 0;
            Row header = sheet.createRow(rowIdx++);
            header.createCell(0).setCellValue("Id");
            header.createCell(1).setCellValue("Title");
            header.createCell(2).setCellValue("Event Name");
            header.createCell(3).setCellValue("Event Type");
            header.createCell(4).setCellValue("Modality");
            header.createCell(5).setCellValue("City");
            header.createCell(6).setCellValue("Country");
            header.createCell(7).setCellValue("Start Date");
            header.createCell(8).setCellValue("End Date");
            header.createCell(9).setCellValue("Progress Report");
            header.createCell(10).setCellValue("ANID Code");
            header.createCell(11).setCellValue("Clusters");
            header.createCell(12).setCellValue("Participants");
            header.createCell(13).setCellValue("# Men");
            header.createCell(14).setCellValue("# Women");

            for (ParticipacionEventoCientifico item : items) {
                ParticipacionEventoCientificoDTO dto = toDto(item, textosMap, false);
                List<ParticipacionProducto> participaciones = participacionProductoRepository.findByProductoId(item.getId());
                String participantsStr = "";
                for (ParticipacionProducto pp : participaciones) {
                    String part = formatParticipantWithTipoRRHH(pp);
                    participantsStr = appendWithSeparator(participantsStr, part);
                }

                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(dto.getId() != null ? dto.getId().toString() : "");
                row.createCell(1).setCellValue(dto.getDescripcion() != null ? dto.getDescripcion() : "");
                row.createCell(2).setCellValue(dto.getEventName() != null ? dto.getEventName() : "");
                row.createCell(3).setCellValue(dto.getTipoParticipacionEvento() != null
                        && dto.getTipoParticipacionEvento().getIdDescripcion() != null
                        ? dto.getTipoParticipacionEvento().getIdDescripcion() : "");
                row.createCell(4).setCellValue(formatModalityForExport(item, textosMap));
                row.createCell(5).setCellValue(dto.getCiudad() != null ? dto.getCiudad() : "");
                row.createCell(6).setCellValue(dto.getPais() != null && dto.getPais().getIdDescripcion() != null
                        ? dto.getPais().getIdDescripcion() : "");
                row.createCell(7).setCellValue(dto.getFechaInicio() != null ? dto.getFechaInicio() : "");
                row.createCell(8).setCellValue(dto.getFechaTermino() != null ? dto.getFechaTermino() : "");
                row.createCell(9).setCellValue(dto.getProgressReport() != null ? dto.getProgressReport() : "");
                row.createCell(10).setCellValue(dto.getCodigoANID() != null ? dto.getCodigoANID() : "");
                row.createCell(11).setCellValue(formatClustersAsRoman(dto.getCluster()));
                row.createCell(12).setCellValue(participantsStr);
                row.createCell(13).setCellValue(countParticipantsByGender(participaciones, "M"));
                row.createCell(14).setCellValue(countParticipantsByGender(participaciones, "F"));
            }

            for (int i = 0; i <= 14; i++) {
                sheet.autoSizeColumn(i);
            }

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=participation-scientific-events.xlsx");
            workbook.write(response.getOutputStream());
            workbook.close();
            response.flushBuffer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ParticipacionEventoCientificoDTO> getParticipationScientificEvent(@PathVariable Long id) {
        Long idRRHH = userService.getCurrentUserIdRRHH().orElse(null);
        String userName = userService.getCurrentUsername().orElse(null);
        return repository.findVisibleByIdAndUserIdRRHH(id, idRRHH, userName)
                .map(item -> ResponseEntity.ok(toDto(item, null, true)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Transactional
    public ResponseEntity<ParticipacionEventoCientificoDTO> createParticipationScientificEvent(@RequestBody ParticipacionEventoCientificoDTO dto) {
        try {
            ParticipacionEventoCientifico entity = new ParticipacionEventoCientifico();
            applyDtoToEntity(dto, entity, false);
            userService.getCurrentUsername().ifPresent(entity::setUsername);

            TipoProducto tipoProducto = tipoProductoRepository.findById(16L)
                    .orElseThrow(() -> new IllegalStateException("TipoProducto with id=16 (PARTICIPACION_EVENTO_CIENTIFICO) not found"));
            entity.setTipoProducto(tipoProducto);

            ParticipacionEventoCientifico saved = repository.save(entity);
            if (dto.getParticipantes() != null && !dto.getParticipantes().isEmpty()) {
                saveParticipantes(saved, dto.getParticipantes());
            }
            return ResponseEntity.ok(toDto(saved, null, true));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<ParticipacionEventoCientificoDTO> updateParticipationScientificEvent(
            @PathVariable Long id,
            @RequestBody ParticipacionEventoCientificoDTO dto) {

        return repository.findById(id)
                .map(existing -> {
                    applyDtoToEntity(dto, existing, true);

                    if (dto.getParticipantes() != null) {
                        participacionProductoRepository.deleteByProductoId(existing.getId());
                        if (!dto.getParticipantes().isEmpty()) {
                            saveParticipantes(existing, dto.getParticipantes());
                        }
                    }

                    ParticipacionEventoCientifico updated = repository.save(existing);
                    return ResponseEntity.ok(toDto(updated, null, true));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deleteParticipationScientificEvent(@PathVariable Long id) {
        return repository.findById(id)
                .map(item -> {
                    if (item.getLinkPDF() != null && !item.getLinkPDF().trim().isEmpty()) {
                        pdfFileService.deletePdfFile(item.getLinkPDF());
                    }
                    participacionProductoRepository.deleteByProductoId(id);
                    repository.deleteById(id);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private void applyDtoToEntity(ParticipacionEventoCientificoDTO dto, ParticipacionEventoCientifico entity, boolean isUpdate) {
        if (dto.getDescripcion() == null || dto.getDescripcion().trim().isEmpty()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (dto.getEventName() == null || dto.getEventName().trim().isEmpty()) {
            throw new IllegalArgumentException("Event name is required");
        }
        if (dto.getTipoParticipacionEvento() == null || dto.getTipoParticipacionEvento().getId() == null) {
            throw new IllegalArgumentException("Event type is required");
        }
        if (dto.getCiudad() == null || dto.getCiudad().trim().isEmpty()) {
            throw new IllegalArgumentException("City is required");
        }
        if (dto.getFechaInicio() == null || dto.getFechaInicio().trim().isEmpty()) {
            throw new IllegalArgumentException("Start date is required");
        }
        Long tipoParticipacionEventoId = dto.getTipoParticipacionEvento() != null ? dto.getTipoParticipacionEvento().getId() : null;
        boolean modalityRequired = tipoParticipacionEventoId != null && (tipoParticipacionEventoId == 1L || tipoParticipacionEventoId == 2L);
        if (modalityRequired && dto.getIdModalidadPresentacion() == null) {
            throw new IllegalArgumentException("Modality is required for event types 1 and 2");
        }

        if (dto.getDescripcion() != null && !dto.getDescripcion().isEmpty()) {
            if (isUpdate && entity.getDescripcion() != null && !entity.getDescripcion().isEmpty()) {
                textosService.updateTextInBothLanguages(entity.getDescripcion(), dto.getDescripcion(), 2);
            } else {
                entity.setDescripcion(textosService.createTextInBothLanguages(dto.getDescripcion(), 2));
            }
        }

        if (dto.getComentario() != null && !dto.getComentario().isEmpty()) {
            if (isUpdate && entity.getComentario() != null && !entity.getComentario().isEmpty()) {
                textosService.updateTextInBothLanguages(entity.getComentario(), dto.getComentario(), 2);
            } else {
                entity.setComentario(textosService.createTextInBothLanguages(dto.getComentario(), 2));
            }
        }

        entity.setFechaInicio(parseLocalDate(dto.getFechaInicio()));
        entity.setFechaTermino(parseLocalDate(dto.getFechaTermino()));
        entity.setLinkPDF(dto.getLinkPDF());

        // Read-only in UI, but keep backend assignment if provided.
        entity.setProgressReport(dto.getProgressReport());
        entity.setCodigoANID(dto.getCodigoANID());

        if (dto.getBasal() != null && !dto.getBasal().isEmpty()) {
            char basalValue = dto.getBasal().charAt(0);
            if (basalValue == 'S' || basalValue == 's' || basalValue == 'N' || basalValue == 'n') {
                entity.setBasal(Character.toUpperCase(basalValue));
            } else {
                entity.setBasal('N');
            }
        } else {
            entity.setBasal('N');
        }

        entity.setCluster(dto.getCluster());
        entity.setEventName(dto.getEventName());
        entity.setCiudad(dto.getCiudad());
        if (modalityRequired) {
            ModalidadPresentacion modalidad = modalidadPresentacionRepository
                    .findById(dto.getIdModalidadPresentacion())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid modality"));
            entity.setModalidadPresentacion(modalidad);
        } else {
            entity.setModalidadPresentacion(null);
        }

        // No va Name of Research Line in this flow.
        entity.setNameResearchLine("");

        if (dto.getEstadoProducto() != null && dto.getEstadoProducto().getId() != null) {
            estadoProductoRepository.findById(dto.getEstadoProducto().getId())
                    .ifPresent(entity::setEstadoProducto);
        }
        if (dto.getTipoParticipacionEvento() != null && dto.getTipoParticipacionEvento().getId() != null) {
            tipoParticipacionEventoRepository.findById(dto.getTipoParticipacionEvento().getId())
                    .ifPresent(entity::setTipoParticipacionEvento);
        }
        if (dto.getPais() != null && dto.getPais().getCodigo() != null) {
            paisRepository.findById(dto.getPais().getCodigo())
                    .ifPresent(entity::setPais);
        }

        TipoProducto tipoProducto = tipoProductoRepository.findById(16L)
                .orElseThrow(() -> new IllegalStateException("TipoProducto with id=16 (PARTICIPACION_EVENTO_CIENTIFICO) not found"));
        entity.setTipoProducto(tipoProducto);
    }

    private ParticipacionEventoCientificoDTO toDto(ParticipacionEventoCientifico entity, Map<String, String> textosMap, boolean withParticipants) {
        ParticipacionEventoCientificoDTO dto = new ParticipacionEventoCientificoDTO();
        dto.setId(entity.getId());
        dto.setDescripcion(resolveText(entity.getDescripcion(), textosMap));
        dto.setComentario(resolveText(entity.getComentario(), textosMap));
        dto.setFechaInicio(entity.getFechaInicio() != null ? entity.getFechaInicio().toString() : null);
        dto.setFechaTermino(entity.getFechaTermino() != null ? entity.getFechaTermino().toString() : null);
        dto.setProgressReport(entity.getProgressReport());
        dto.setCodigoANID(entity.getCodigoANID());
        dto.setLinkPDF(entity.getLinkPDF());
        dto.setCluster(entity.getCluster());
        dto.setEventName(entity.getEventName());
        dto.setCiudad(entity.getCiudad());
        dto.setIdModalidadPresentacion(
                entity.getModalidadPresentacion() != null ? entity.getModalidadPresentacion().getId() : null
        );
        if (entity.getBasal() != null) {
            dto.setBasal(String.valueOf(entity.getBasal()));
        }

        if (entity.getTipoParticipacionEvento() != null) {
            dto.setTipoParticipacionEvento(new TipoParticipacionEventoDTO(
                    entity.getTipoParticipacionEvento().getId(),
                    resolveText(entity.getTipoParticipacionEvento().getIdDescripcion(), textosMap)
            ));
        }
        if (entity.getEstadoProducto() != null) {
            EstadoProductoDTO estado = new EstadoProductoDTO();
            estado.setId(entity.getEstadoProducto().getId());
            estado.setCodigoDescripcion(entity.getEstadoProducto().getCodigoDescripcion());
            dto.setEstadoProducto(estado);
        }
        if (entity.getPais() != null) {
            PaisDTO paisDTO = new PaisDTO();
            paisDTO.setCodigo(entity.getPais().getCodigo());
            paisDTO.setIdDescripcion(resolveText(entity.getPais().getIdDescripcion(), textosMap));
            dto.setPais(paisDTO);
        }

        if (withParticipants) {
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
        }

        return dto;
    }

    private void saveParticipantes(ParticipacionEventoCientifico entity, List<ParticipanteDTO> participantesDTO) {
        for (ParticipanteDTO pDTO : participantesDTO) {
            if (pDTO.getRrhhId() == null || pDTO.getTipoParticipacionId() == null) {
                continue;
            }

            RRHH rrhh = rrhhRepository.findById(pDTO.getRrhhId()).orElse(null);
            TipoParticipacion tipoParticipacion = tipoParticipacionRepository.findById(pDTO.getTipoParticipacionId()).orElse(null);

            if (rrhh != null && tipoParticipacion != null) {
                Long nextId = participacionProductoRepository.getNextIdForParticipacion(entity.getId(), rrhh.getId());

                ParticipacionProducto participacion = new ParticipacionProducto();
                participacion.setRrhh(rrhh);
                participacion.setProducto(entity);
                participacion.setTipoParticipacion(tipoParticipacion);
                participacion.setOrden(pDTO.getOrden() != null ? pDTO.getOrden() : 0);
                participacion.setCorresponding(pDTO.getCorresponding() != null && pDTO.getCorresponding());

                ParticipacionProductoId id = new ParticipacionProductoId(rrhh.getId(), entity.getId(), nextId);
                participacion.setId(id);

                participacionProductoRepository.save(participacion);
            }
        }
    }

    private LocalDate parseLocalDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            try {
                return LocalDate.parse(dateStr, DateTimeFormatter.ISO_DATE);
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    private String resolveText(String code, Map<String, String> textosMap) {
        if (code == null || code.isEmpty()) {
            return code;
        }
        if (textosMap != null && textosMap.containsKey(code)) {
            return textosMap.get(code);
        }
        return textosService.getTextValue(code, 2, "us").orElse(code);
    }

    private String formatModalityForExport(ParticipacionEventoCientifico item, Map<String, String> textosMap) {
        if (item == null || item.getModalidadPresentacion() == null) {
            return "";
        }
        String raw = item.getModalidadPresentacion().getIdDescripcion();
        return resolveText(raw, textosMap);
    }

    private String appendWithSeparator(String existing, String toAdd) {
        if (toAdd == null || toAdd.trim().isEmpty()) {
            return existing != null ? existing : "";
        }
        if (existing == null || existing.isEmpty()) {
            return toAdd;
        }
        return existing + "; " + toAdd;
    }

    private String formatParticipantWithTipoRRHH(ParticipacionProducto participacion) {
        if (participacion == null || participacion.getRrhh() == null) {
            return "";
        }
        RRHH rrhh = participacion.getRrhh();
        String fullname = rrhh.getFullname() != null ? rrhh.getFullname().trim() : "";
        if (fullname.isEmpty()) {
            return "";
        }

        String tipoRRHH = "";
        if (rrhh.getTipoRRHH() != null) {
            if (rrhh.getTipoRRHH().getDescripcion() != null && !rrhh.getTipoRRHH().getDescripcion().trim().isEmpty()) {
                tipoRRHH = rrhh.getTipoRRHH().getDescripcion().trim();
            } else if (rrhh.getTipoRRHH().getCodigoDescripcion() != null) {
                tipoRRHH = rrhh.getTipoRRHH().getCodigoDescripcion().trim();
            }
        }
        return tipoRRHH.isEmpty() ? fullname : fullname + " (" + tipoRRHH + ")";
    }

    private String formatClustersAsRoman(String cluster) {
        if (cluster == null || cluster.isEmpty()) {
            return "";
        }
        return java.util.Arrays.stream(cluster.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    switch (s) {
                        case "1":
                            return "I";
                        case "2":
                            return "II";
                        case "3":
                            return "III";
                        case "4":
                            return "IV";
                        case "5":
                            return "V";
                        default:
                            return s;
                    }
                })
                .collect(Collectors.joining(", "));
    }

    /** Unique RRHH per record with codigoGenero M or F (case-insensitive). */
    private int countParticipantsByGender(List<ParticipacionProducto> participaciones, String genderCode) {
        if (participaciones == null || participaciones.isEmpty() || genderCode == null || genderCode.isBlank()) {
            return 0;
        }
        Set<Long> seenRrhhIds = new HashSet<>();
        int count = 0;
        for (ParticipacionProducto pp : participaciones) {
            if (pp == null || pp.getRrhh() == null) {
                continue;
            }
            RRHH rrhh = pp.getRrhh();
            Long rrhhId = rrhh.getId();
            if (rrhhId != null) {
                if (seenRrhhIds.contains(rrhhId)) {
                    continue;
                }
                seenRrhhIds.add(rrhhId);
            }
            String codigoGenero = rrhh.getCodigoGenero();
            if (codigoGenero != null && genderCode.equalsIgnoreCase(codigoGenero.trim())) {
                count++;
            }
        }
        return count;
    }
}
