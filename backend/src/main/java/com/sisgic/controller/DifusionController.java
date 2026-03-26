package com.sisgic.controller;

import com.sisgic.dto.DifusionDTO;
import com.sisgic.dto.EstadoProductoDTO;
import com.sisgic.dto.ParticipanteDTO;
import com.sisgic.dto.PaisDTO;
import com.sisgic.dto.TipoDifusionDTO;
import com.sisgic.dto.TipoProductoDTO;
import com.sisgic.entity.*;
import com.sisgic.repository.*;
import com.sisgic.service.TextosService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/outreach-activities")
@CrossOrigin(origins = "*")
public class DifusionController {

    @Autowired
    private DifusionRepository difusionRepository;

    @Autowired
    private TipoDifusionRepository tipoDifusionRepository;

    @Autowired
    private PaisRepository paisRepository;

    @Autowired
    private TipoProductoRepository tipoProductoRepository;

    @Autowired
    private EstadoProductoRepository estadoProductoRepository;

    @Autowired
    private RRHHRepository rrhhRepository;

    @Autowired
    private TipoParticipacionRepository tipoParticipacionRepository;

    @Autowired
    private ParticipacionProductoRepository participacionProductoRepository;

    @Autowired
    private PublicoObjetivoRepository publicoObjetivoRepository;

    @Autowired
    private TextosService textosService;

    @Autowired
    private com.sisgic.service.PdfFileService pdfFileService;

    @Autowired
    private com.sisgic.service.UserService userService;

    /**
     * Obtiene todas las actividades de difusión con paginación
     */
    @GetMapping
    public ResponseEntity<Page<DifusionDTO>> getOutreachActivities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc") ?
            Sort.by(sortBy).descending() :
            Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        
        // Obtener idRRHH y userName del usuario conectado
        Long idRRHH = userService.getCurrentUserIdRRHH().orElse(null);
        String userName = userService.getCurrentUsername().orElse(null);
        Page<Difusion> difusiones = difusionRepository.findVisibleByUserIdRRHH(idRRHH, userName, pageable);

        // Cargar todos los textos de una vez para optimizar consultas
        List<Difusion> difusionesList = difusiones.getContent();
        List<String> codigosTexto = new ArrayList<>();
        for (Difusion d : difusionesList) {
            if (d.getDescripcion() != null && !d.getDescripcion().isEmpty()) {
                codigosTexto.add(d.getDescripcion());
            }
            if (d.getComentario() != null && !d.getComentario().isEmpty()) {
                codigosTexto.add(d.getComentario());
            }
        }
        Map<String, String> textosMap = textosService.getTextValuesBatch(codigosTexto, 2, "us");

        // Para listas, no cargar participantes para mejorar rendimiento
        Page<DifusionDTO> difusionesDTO = difusiones.map(d -> convertToDTOWithoutParticipants(d, textosMap));

        return ResponseEntity.ok(difusionesDTO);
    }

    /**
     * Exporta actividades de difusión visibles a Excel.
     */
    @GetMapping("/export")
    public void exportOutreachActivitiesToExcel(
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir,
            HttpServletResponse response) {
        try {
            Sort sort = sortDir.equalsIgnoreCase("DESC")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

            Long idRRHH = userService.getCurrentUserIdRRHH().orElse(null);
            String userName = userService.getCurrentUsername().orElse(null);
            List<Difusion> activities = difusionRepository
                .findVisibleByUserIdRRHH(idRRHH, userName, PageRequest.of(0, 100000, sort))
                .getContent();

            Map<Long, String> targetAudienceDescriptions = new HashMap<>();
            for (PublicoObjetivo po : publicoObjetivoRepository.findAllByOrderByIdAsc()) {
                String desc = po.getIdDescripcion() != null
                    ? textosService.getTextValue(po.getIdDescripcion(), 2, "us").orElse(po.getIdDescripcion())
                    : "";
                if (po.getId() != null) {
                    targetAudienceDescriptions.put(po.getId(), desc);
                }
            }

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Outreach Activities");

            Row headerRow = sheet.createRow(0);
            String[] headers = {
                "Id",
                "Name",
                "Description",
                "Diffusion Type",
                "Country",
                "Place",
                "Number of Attendees",
                "Duration",
                "Start Date",
                "End Date",
                "Target Audience",
                "Clusters",
                "Progress Report (Period)",
                "ANID Code",
                "creationDate",
                "Main Responsible",
                "Participants"
            };

            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            int rowNum = 1;
            for (Difusion activity : activities) {
                Row row = sheet.createRow(rowNum++);
                DifusionDTO dto = convertToDTO(activity);

                List<ParticipacionProducto> participaciones = participacionProductoRepository.findByProductoId(activity.getId());
                String mainResponsible = participaciones.stream()
                    .filter(pp -> pp.getTipoParticipacion() != null && pp.getTipoParticipacion().getId() != null
                        && pp.getTipoParticipacion().getId().longValue() == 20L)
                    .map(pp -> pp.getRrhh() != null ? pp.getRrhh().getFullname() : null)
                    .filter(name -> name != null && !name.trim().isEmpty())
                    .distinct()
                    .collect(Collectors.joining(", "));

                String participants = participaciones.stream()
                    .filter(pp -> pp.getTipoParticipacion() == null || pp.getTipoParticipacion().getId() == null
                        || pp.getTipoParticipacion().getId().longValue() != 20L)
                    .map(pp -> pp.getRrhh() != null ? pp.getRrhh().getFullname() : null)
                    .filter(name -> name != null && !name.trim().isEmpty())
                    .distinct()
                    .collect(Collectors.joining(", "));

                String targetAudience = "";
                if (dto.getPublicoObjetivo() != null && !dto.getPublicoObjetivo().trim().isEmpty()) {
                    targetAudience = java.util.Arrays.stream(dto.getPublicoObjetivo().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(idStr -> {
                            try {
                                Long id = Long.parseLong(idStr);
                                return targetAudienceDescriptions.getOrDefault(id, idStr);
                            } catch (NumberFormatException e) {
                                return idStr;
                            }
                        })
                        .collect(Collectors.joining(", "));
                }

                String cluster = "";
                if (dto.getCluster() != null && !dto.getCluster().trim().isEmpty()) {
                    cluster = java.util.Arrays.stream(dto.getCluster().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(this::mapClusterLabel)
                        .collect(Collectors.joining(", "));
                }

                String diffusionType = dto.getTipoDifusion() != null && dto.getTipoDifusion().getIdDescripcion() != null
                    ? textosService.getTextValue(dto.getTipoDifusion().getIdDescripcion(), 2, "us")
                        .orElse(dto.getTipoDifusion().getIdDescripcion())
                    : "";
                String country = dto.getPais() != null && dto.getPais().getIdDescripcion() != null
                    ? textosService.getTextValue(dto.getPais().getIdDescripcion(), 2, "us")
                        .orElse(dto.getPais().getIdDescripcion())
                    : "";

                row.createCell(0).setCellValue(dto.getId() != null ? dto.getId().toString() : "");
                row.createCell(1).setCellValue(dto.getDescripcion() != null ? dto.getDescripcion() : "");
                row.createCell(2).setCellValue(dto.getComentario() != null ? dto.getComentario() : "");
                row.createCell(3).setCellValue(diffusionType);
                row.createCell(4).setCellValue(country);
                row.createCell(5).setCellValue(dto.getLugar() != null ? dto.getLugar() : "");
                row.createCell(6).setCellValue(dto.getNumAsistentes() != null ? dto.getNumAsistentes().toString() : "");
                row.createCell(7).setCellValue(dto.getDuracion() != null ? dto.getDuracion().toString() : "");
                row.createCell(8).setCellValue(dto.getFechaInicio() != null ? dto.getFechaInicio() : "");
                row.createCell(9).setCellValue(dto.getFechaTermino() != null ? dto.getFechaTermino() : "");
                row.createCell(10).setCellValue(targetAudience);
                row.createCell(11).setCellValue(cluster);
                row.createCell(12).setCellValue(dto.getProgressReport() != null ? dto.getProgressReport() : "");
                row.createCell(13).setCellValue(dto.getCodigoANID() != null ? dto.getCodigoANID() : "");
                row.createCell(14).setCellValue(dto.getCreatedAt() != null ? dto.getCreatedAt() : "");
                row.createCell(15).setCellValue(mainResponsible);
                row.createCell(16).setCellValue(participants);
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=outreach-activities.xlsx");
            workbook.write(response.getOutputStream());
            workbook.close();
        } catch (Exception e) {
            throw new RuntimeException("Error generating outreach activities Excel file", e);
        }
    }

    /**
     * Obtiene una actividad de difusión específica por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<DifusionDTO> getOutreachActivity(@PathVariable Long id) {
        Long idRRHH = userService.getCurrentUserIdRRHH().orElse(null);
        String userName = userService.getCurrentUsername().orElse(null);
        return difusionRepository.findVisibleByIdAndUserIdRRHH(id, idRRHH, userName)
            .map(difusion -> ResponseEntity.ok(convertToDTO(difusion)))
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Crea una nueva actividad de difusión
     */
    @PostMapping
    @Transactional
    public ResponseEntity<DifusionDTO> createOutreachActivity(@RequestBody DifusionDTO dto) {
        try {
            Difusion difusion = convertFromDTO(dto);
            // Establecer el username del usuario actual solo al crear
            userService.getCurrentUsername().ifPresent(difusion::setUsername);
            Difusion saved = difusionRepository.save(difusion);

            if (dto.getParticipantes() != null && !dto.getParticipantes().isEmpty()) {
                saveParticipantes(saved, dto.getParticipantes());
            }

            DifusionDTO resultDTO = convertToDTO(saved);
            return ResponseEntity.ok(resultDTO);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Actualiza una actividad de difusión existente
     */
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<DifusionDTO> updateOutreachActivity(
            @PathVariable Long id,
            @RequestBody DifusionDTO dto) {

        return difusionRepository.findById(id)
            .map(existingDifusion -> {
                // Actualizar campos de ProductoCientifico
                if (dto.getDescripcion() != null && !dto.getDescripcion().isEmpty()) {
                    if (existingDifusion.getDescripcion() != null && !existingDifusion.getDescripcion().isEmpty()) {
                        textosService.updateTextInBothLanguages(existingDifusion.getDescripcion(), dto.getDescripcion(), 2);
                    } else {
                        String codigoDescripcion = textosService.createTextInBothLanguages(dto.getDescripcion(), 2);
                        existingDifusion.setDescripcion(codigoDescripcion);
                    }
                }
                if (dto.getComentario() != null && !dto.getComentario().isEmpty()) {
                    if (existingDifusion.getComentario() != null && !existingDifusion.getComentario().isEmpty()) {
                        textosService.updateTextInBothLanguages(existingDifusion.getComentario(), dto.getComentario(), 2);
                    } else {
                        String codigoComentario = textosService.createTextInBothLanguages(dto.getComentario(), 2);
                        existingDifusion.setComentario(codigoComentario);
                    }
                }
                // fechaInicio es obligatorio
                if (dto.getFechaInicio() == null || dto.getFechaInicio().isEmpty()) {
                    throw new IllegalArgumentException("fechaInicio is required");
                }
                existingDifusion.setFechaInicio(parseLocalDate(dto.getFechaInicio()));
                if (dto.getFechaTermino() != null) {
                    existingDifusion.setFechaTermino(parseLocalDate(dto.getFechaTermino()));
                }
                if (dto.getUrlDocumento() != null) {
                    existingDifusion.setUrlDocumento(dto.getUrlDocumento());
                }
                if (dto.getLinkVisualizacion() != null) {
                    existingDifusion.setLinkVisualizacion(dto.getLinkVisualizacion());
                }
                // Actualizar linkPDF siempre (puede ser null para limpiarlo)
                existingDifusion.setLinkPDF(dto.getLinkPDF());
                existingDifusion.setProgressReport(dto.getProgressReport());
                if (dto.getCodigoANID() != null) {
                    existingDifusion.setCodigoANID(dto.getCodigoANID());
                }
                if (dto.getBasal() != null && !dto.getBasal().isEmpty()) {
                    char basalValue = dto.getBasal().charAt(0);
                    if (basalValue == 'S' || basalValue == 's' || basalValue == 'N' || basalValue == 'n') {
                        existingDifusion.setBasal(Character.toUpperCase(basalValue));
                    } else {
                        existingDifusion.setBasal('N');
                    }
                } else {
                    existingDifusion.setBasal('N');
                }
                if (dto.getLineasInvestigacion() != null) {
                    existingDifusion.setLineasInvestigacion(dto.getLineasInvestigacion());
                }
                if (dto.getCluster() != null) {
                    existingDifusion.setCluster(dto.getCluster());
                }

                // Actualizar relaciones de ProductoCientifico
                if (dto.getTipoProducto() != null && dto.getTipoProducto().getId() != null) {
                    tipoProductoRepository.findById(dto.getTipoProducto().getId())
                        .ifPresent(existingDifusion::setTipoProducto);
                }
                if (dto.getEstadoProducto() != null && dto.getEstadoProducto().getId() != null) {
                    estadoProductoRepository.findById(dto.getEstadoProducto().getId())
                        .ifPresent(existingDifusion::setEstadoProducto);
                }

                // Actualizar campos específicos de Difusion
                if (dto.getTipoDifusion() != null && dto.getTipoDifusion().getId() != null) {
                    tipoDifusionRepository.findById(dto.getTipoDifusion().getId())
                        .ifPresent(existingDifusion::setTipoDifusion);
                }
                if (dto.getPais() != null && dto.getPais().getCodigo() != null) {
                    paisRepository.findById(dto.getPais().getCodigo())
                        .ifPresent(existingDifusion::setPais);
                }
                if (dto.getLugar() != null) {
                    existingDifusion.setLugar(dto.getLugar());
                }
                if (dto.getNumAsistentes() != null) {
                    existingDifusion.setNumAsistentes(dto.getNumAsistentes());
                }
                if (dto.getDuracion() != null) {
                    existingDifusion.setDuracion(dto.getDuracion());
                }
                if (dto.getPublicoObjetivo() != null) {
                    existingDifusion.setPublicoObjetivo(dto.getPublicoObjetivo());
                }
                if (dto.getCiudad() != null) {
                    existingDifusion.setCiudad(dto.getCiudad());
                }
                if (dto.getLink() != null) {
                    existingDifusion.setLink(dto.getLink());
                }

                // Actualizar participantes
                if (dto.getParticipantes() != null) {
                    participacionProductoRepository.deleteByProductoId(existingDifusion.getId());
                    if (!dto.getParticipantes().isEmpty()) {
                        saveParticipantes(existingDifusion, dto.getParticipantes());
                    }
                }

                Difusion updated = difusionRepository.save(existingDifusion);
                return ResponseEntity.ok(convertToDTO(updated));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Elimina una actividad de difusión
     */
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deleteOutreachActivity(@PathVariable Long id) {
        return difusionRepository.findById(id)
            .map(difusion -> {
                // Eliminar archivo PDF asociado si existe
                if (difusion.getLinkPDF() != null && !difusion.getLinkPDF().trim().isEmpty()) {
                    pdfFileService.deletePdfFile(difusion.getLinkPDF());
                }
                // Eliminar participantes asociados
                participacionProductoRepository.deleteByProductoId(id);
                // Eliminar el registro
                difusionRepository.deleteById(id);
                return ResponseEntity.noContent().<Void>build();
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Convierte una entidad Difusion a DTO (sin participantes - para listas)
     */
    private DifusionDTO convertToDTOWithoutParticipants(Difusion difusion) {
        return convertToDTOWithoutParticipants(difusion, null);
    }
    
    /**
     * Convierte una entidad Difusion a DTO (sin participantes - para listas) con textos precargados
     */
    private DifusionDTO convertToDTOWithoutParticipants(Difusion difusion, Map<String, String> textosMap) {
        DifusionDTO dto = convertToDTOBase(difusion, textosMap);
        // No cargar participantes para mejorar rendimiento en listas
        dto.setParticipantes(null);
        return dto;
    }

    /**
     * Convierte una entidad Difusion a DTO (con participantes - para detalles)
     */
    private DifusionDTO convertToDTO(Difusion difusion) {
        DifusionDTO dto = convertToDTOBase(difusion);
        
        // Cargar participantes solo cuando se necesita (detalles)
        List<ParticipacionProducto> participaciones = participacionProductoRepository.findByProductoId(difusion.getId());
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

    /**
     * Método base para convertir Difusion a DTO (sin participantes)
     */
    private DifusionDTO convertToDTOBase(Difusion difusion) {
        return convertToDTOBase(difusion, null);
    }
    
    /**
     * Método base para convertir Difusion a DTO (sin participantes) con textos precargados
     */
    private DifusionDTO convertToDTOBase(Difusion difusion, Map<String, String> textosMap) {
        DifusionDTO dto = new DifusionDTO();

        // Campos de ProductoCientifico
        dto.setId(difusion.getId());
        if (difusion.getDescripcion() != null && !difusion.getDescripcion().isEmpty()) {
            String descripcion = textosMap != null && textosMap.containsKey(difusion.getDescripcion())
                ? textosMap.get(difusion.getDescripcion())
                : textosService.getTextValue(difusion.getDescripcion(), 2, "us").orElse(difusion.getDescripcion());
            dto.setDescripcion(descripcion);
        }
        if (difusion.getComentario() != null && !difusion.getComentario().isEmpty()) {
            String comentario = textosMap != null && textosMap.containsKey(difusion.getComentario())
                ? textosMap.get(difusion.getComentario())
                : textosService.getTextValue(difusion.getComentario(), 2, "us").orElse(difusion.getComentario());
            dto.setComentario(comentario);
        }
        dto.setFechaInicio(difusion.getFechaInicio() != null ? difusion.getFechaInicio().toString() : null);
        dto.setFechaTermino(difusion.getFechaTermino() != null ? difusion.getFechaTermino().toString() : null);
        dto.setUrlDocumento(difusion.getUrlDocumento());
        dto.setLinkVisualizacion(difusion.getLinkVisualizacion());
        dto.setLinkPDF(difusion.getLinkPDF());
        dto.setProgressReport(difusion.getProgressReport());
        dto.setCodigoANID(difusion.getCodigoANID());
        // Normalizar basal: convertir '1' a 'S' y '0' a 'N', mantener 'S'/'N' como están
        if (difusion.getBasal() != null) {
            char basalChar = difusion.getBasal();
            if (basalChar == '1') {
                dto.setBasal("S");
            } else if (basalChar == '0') {
                dto.setBasal("N");
            } else {
                dto.setBasal(String.valueOf(basalChar));
            }
        } else {
            dto.setBasal(null);
        }
        dto.setLineasInvestigacion(difusion.getLineasInvestigacion());
        dto.setCluster(difusion.getCluster());
        dto.setParticipantesNombres(difusion.getParticipantesNombres());
        dto.setCreatedAt(difusion.getCreatedAt() != null ? difusion.getCreatedAt().toString() : null);
        dto.setUpdatedAt(difusion.getUpdatedAt() != null ? difusion.getUpdatedAt().toString() : null);

        // Relaciones de ProductoCientifico
        if (difusion.getTipoProducto() != null) {
            TipoProductoDTO tipoDTO = new TipoProductoDTO();
            tipoDTO.setId(difusion.getTipoProducto().getId());
            tipoDTO.setIdDescripcion(difusion.getTipoProducto().getIdDescripcion());
            tipoDTO.setDescripcion(difusion.getTipoProducto().getDescripcion());
            dto.setTipoProducto(tipoDTO);
        }

        if (difusion.getEstadoProducto() != null) {
            EstadoProductoDTO estadoDTO = new EstadoProductoDTO();
            estadoDTO.setId(difusion.getEstadoProducto().getId());
            estadoDTO.setCodigoDescripcion(difusion.getEstadoProducto().getCodigoDescripcion());
            estadoDTO.setCreatedAt(difusion.getEstadoProducto().getCreatedAt() != null ?
                difusion.getEstadoProducto().getCreatedAt().toString() : null);
            estadoDTO.setUpdatedAt(difusion.getEstadoProducto().getUpdatedAt() != null ?
                difusion.getEstadoProducto().getUpdatedAt().toString() : null);
            dto.setEstadoProducto(estadoDTO);
        }

        // Campos específicos de Difusion
        if (difusion.getTipoDifusion() != null) {
            TipoDifusionDTO tipoDifusionDTO = new TipoDifusionDTO();
            tipoDifusionDTO.setId(difusion.getTipoDifusion().getId());
            tipoDifusionDTO.setIdDescripcion(difusion.getTipoDifusion().getIdDescripcion());
            dto.setTipoDifusion(tipoDifusionDTO);
        }

        if (difusion.getPais() != null) {
            PaisDTO paisDTO = new PaisDTO();
            paisDTO.setCodigo(difusion.getPais().getCodigo());
            paisDTO.setIdDescripcion(difusion.getPais().getIdDescripcion());
            dto.setPais(paisDTO);
        }

        dto.setLugar(difusion.getLugar());
        dto.setNumAsistentes(difusion.getNumAsistentes());
        dto.setDuracion(difusion.getDuracion());
        dto.setPublicoObjetivo(difusion.getPublicoObjetivo());
        dto.setCiudad(difusion.getCiudad());
        dto.setLink(difusion.getLink());
        dto.setMainResponsible(difusion.getMainResponsible());

        return dto;
    }

    /**
     * Convierte un DTO a entidad Difusion
     */
    private Difusion convertFromDTO(DifusionDTO dto) {
        Difusion difusion = new Difusion();

        // Campos de ProductoCientifico
        if (dto.getDescripcion() != null && !dto.getDescripcion().isEmpty()) {
            String codigoDescripcion = textosService.createTextInBothLanguages(dto.getDescripcion(), 2);
            difusion.setDescripcion(codigoDescripcion);
        }
        if (dto.getComentario() != null && !dto.getComentario().isEmpty()) {
            String codigoComentario = textosService.createTextInBothLanguages(dto.getComentario(), 2);
            difusion.setComentario(codigoComentario);
        }
        // fechaInicio es obligatorio
        if (dto.getFechaInicio() == null || dto.getFechaInicio().isEmpty()) {
            throw new IllegalArgumentException("fechaInicio is required");
        }
        difusion.setFechaInicio(parseLocalDate(dto.getFechaInicio()));
        if (dto.getFechaTermino() != null) {
            difusion.setFechaTermino(parseLocalDate(dto.getFechaTermino()));
        }
        difusion.setUrlDocumento(dto.getUrlDocumento());
        difusion.setLinkVisualizacion(dto.getLinkVisualizacion());
        difusion.setLinkPDF(dto.getLinkPDF());
        difusion.setProgressReport(dto.getProgressReport());
        difusion.setCodigoANID(dto.getCodigoANID());
        if (dto.getBasal() != null && !dto.getBasal().isEmpty()) {
            char basalValue = dto.getBasal().charAt(0);
            if (basalValue == 'S' || basalValue == 's' || basalValue == 'N' || basalValue == 'n') {
                difusion.setBasal(Character.toUpperCase(basalValue));
            } else {
                difusion.setBasal('S');
            }
        } else {
            difusion.setBasal('S');
        }
        difusion.setLineasInvestigacion(dto.getLineasInvestigacion());
        difusion.setCluster(dto.getCluster());

        // Relaciones de ProductoCientifico
        if (dto.getTipoProducto() != null && dto.getTipoProducto().getId() != null) {
            tipoProductoRepository.findById(dto.getTipoProducto().getId())
                .ifPresent(difusion::setTipoProducto);
        }

        if (dto.getEstadoProducto() != null && dto.getEstadoProducto().getId() != null) {
            estadoProductoRepository.findById(dto.getEstadoProducto().getId())
                .ifPresent(difusion::setEstadoProducto);
        }

        // Campos específicos de Difusion
        if (dto.getTipoDifusion() != null && dto.getTipoDifusion().getId() != null) {
            tipoDifusionRepository.findById(dto.getTipoDifusion().getId())
                .ifPresent(difusion::setTipoDifusion);
        }
        if (dto.getPais() != null && dto.getPais().getCodigo() != null) {
            paisRepository.findById(dto.getPais().getCodigo())
                .ifPresent(difusion::setPais);
        }
        difusion.setLugar(dto.getLugar());
        difusion.setNumAsistentes(dto.getNumAsistentes());
        difusion.setDuracion(dto.getDuracion());
        difusion.setPublicoObjetivo(dto.getPublicoObjetivo());
        difusion.setCiudad(dto.getCiudad());
        difusion.setLink(dto.getLink());

        return difusion;
    }

    /**
     * Convierte un string a LocalDate
     */
    private LocalDate parseLocalDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            try {
                return LocalDate.parse(dateStr, DateTimeFormatter.ISO_DATE);
            } catch (Exception e2) {
                return null;
            }
        }
    }

    private String mapClusterLabel(String clusterId) {
        switch (clusterId) {
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
                return clusterId;
        }
    }

    /**
     * Guarda los participantes de una actividad de difusión
     */
    private void saveParticipantes(Difusion difusion, List<ParticipanteDTO> participantesDTO) {
        for (ParticipanteDTO pDTO : participantesDTO) {
            if (pDTO.getRrhhId() == null || pDTO.getTipoParticipacionId() == null) {
                continue;
            }

            RRHH rrhh = rrhhRepository.findById(pDTO.getRrhhId()).orElse(null);
            TipoParticipacion tipoParticipacion = tipoParticipacionRepository.findById(pDTO.getTipoParticipacionId()).orElse(null);

            if (rrhh != null && tipoParticipacion != null) {
                // Obtener el siguiente ID correlativo para esta combinación (idProducto, idRRHH)
                Long nextId = participacionProductoRepository.getNextIdForParticipacion(
                    difusion.getId(), 
                    rrhh.getId()
                );
                
                ParticipacionProducto participacion = new ParticipacionProducto();
                participacion.setRrhh(rrhh);
                participacion.setProducto(difusion);
                participacion.setTipoParticipacion(tipoParticipacion);
                participacion.setOrden(pDTO.getOrden() != null ? pDTO.getOrden() : 0);
                participacion.setCorresponding(pDTO.getCorresponding() != null && pDTO.getCorresponding());

                // Establecer el ID compuesto con el correlativo
                ParticipacionProductoId id = new ParticipacionProductoId(
                    rrhh.getId(), 
                    difusion.getId(), 
                    nextId
                );
                participacion.setId(id);

                participacionProductoRepository.save(participacion);
            }
        }
    }
}

