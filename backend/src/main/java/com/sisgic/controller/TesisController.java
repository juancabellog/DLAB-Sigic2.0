package com.sisgic.controller;

import com.sisgic.dto.TesisDTO;
import com.sisgic.dto.EstadoProductoDTO;
import com.sisgic.dto.ParticipanteDTO;
import com.sisgic.dto.InstitucionDTO;
import com.sisgic.dto.GradoAcademicoDTO;
import com.sisgic.dto.EstadoTesisDTO;
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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/thesis")
@CrossOrigin(origins = "*")
public class TesisController {

    @Autowired
    private TesisRepository tesisRepository;

    @Autowired
    private InstitucionRepository institucionRepository;

    @Autowired
    private GradoAcademicoRepository gradoAcademicoRepository;

    @Autowired
    private EstadoTesisRepository estadoTesisRepository;

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
    private TipoSectorRepository tipoSectorRepository;

    @Autowired
    private TextosService textosService;

    @Autowired
    private com.sisgic.service.PdfFileService pdfFileService;

    @Autowired
    private com.sisgic.service.UserService userService;

    /**
     * Obtiene todas las tesis con paginación
     */
    @GetMapping
    public ResponseEntity<Page<TesisDTO>> getThesis(
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
        Page<Tesis> tesis = tesisRepository.findVisibleByUserIdRRHH(idRRHH, userName, pageable);

        // Cargar todos los textos de una vez para optimizar consultas
        List<Tesis> tesisList = tesis.getContent();
        List<String> codigosTexto = new ArrayList<>();
        for (Tesis t : tesisList) {
            if (t.getDescripcion() != null && !t.getDescripcion().isEmpty()) {
                codigosTexto.add(t.getDescripcion());
            }
            if (t.getComentario() != null && !t.getComentario().isEmpty()) {
                codigosTexto.add(t.getComentario());
            }
        }
        Map<String, String> textosMap = textosService.getTextValuesBatch(codigosTexto, 2, "us");

        // Para listas, no cargar participantes para mejorar rendimiento
        Page<TesisDTO> tesisDTO = tesis.map(t -> convertToDTOWithoutParticipants(t, textosMap));

        return ResponseEntity.ok(tesisDTO);
    }

    /**
     * Exporta las tesis visibles a Excel (usa los mismos filtros básicos de visibilidad)
     */
    @GetMapping("/export")
    @Transactional(readOnly = true)
    public void exportThesisToExcel(
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            HttpServletResponse response) {

        try {
            Sort sort = sortDir.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() :
                Sort.by(sortBy).ascending();

            Long idRRHH = userService.getCurrentUserIdRRHH().orElse(null);
            String userName = userService.getCurrentUsername().orElse(null);
            List<Tesis> tesisList = tesisRepository
                .findVisibleByUserIdRRHH(idRRHH, userName, Pageable.unpaged(sort))
                .getContent();

            // Mapear tipos de sector (id -> descripción)
            Map<Long, String> sectorDescriptions = new HashMap<>();
            tipoSectorRepository.findAll().forEach(ts -> {
                if (ts.getId() != null && ts.getIdDescripcion() != null) {
                    String desc = textosService
                        .getTextValue(ts.getIdDescripcion(), 2, "us")
                        .orElse(ts.getIdDescripcion());
                    sectorDescriptions.put(ts.getId(), desc);
                }
            });

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Thesis Students");

            int rowIdx = 0;
            Row header = sheet.createRow(rowIdx++);
            header.createCell(0).setCellValue("Id");
            header.createCell(1).setCellValue("Title");
            header.createCell(2).setCellValue("Citation");
            header.createCell(3).setCellValue("Degree Granting Institution");
            header.createCell(4).setCellValue("Academic Degree");
            header.createCell(5).setCellValue("Host Institution");
            header.createCell(6).setCellValue("Thesis Status");
            header.createCell(7).setCellValue("Program Start Date");
            header.createCell(8).setCellValue("QE Date");
            header.createCell(9).setCellValue("Degree Award Date");
            header.createCell(10).setCellValue("Sector Types");
            header.createCell(11).setCellValue("Clusters");
            header.createCell(12).setCellValue("Basal");
            header.createCell(13).setCellValue("Period");
            header.createCell(14).setCellValue("ANID Code");
            header.createCell(15).setCellValue("Principal Supervisor Name");
            header.createCell(16).setCellValue("Principal Supervisor Gender");
            header.createCell(17).setCellValue("Principal Supervisor Type");
            header.createCell(18).setCellValue("Supervisor Name");
            header.createCell(19).setCellValue("Supervisor Gender");
            header.createCell(20).setCellValue("Supervisor Type");
            header.createCell(21).setCellValue("Student Name");
            header.createCell(22).setCellValue("Student Gender");
            header.createCell(23).setCellValue("Student Type");

            Map<String, String> textosMap = Map.of();

            for (Tesis tesis : tesisList) {
                TesisDTO dto = convertToDTOBase(tesis, textosMap);
                Row row = sheet.createRow(rowIdx++);

                // ID (id interno)
                row.createCell(0).setCellValue(dto.getId() != null ? dto.getId().toString() : "");

                // Title
                row.createCell(1).setCellValue(
                    dto.getNombreCompletoTitulo() != null ? dto.getNombreCompletoTitulo() :
                    (dto.getDescripcion() != null ? dto.getDescripcion() : "")
                );

                // Citation (descripcion)
                row.createCell(2).setCellValue(dto.getDescripcion() != null ? dto.getDescripcion() : "");

                // Degree Granting Institution (institucionOG)
                String degreeGrantingInst = "";
                if (dto.getInstitucionOG() != null) {
                    if (dto.getInstitucionOG().getDescripcion() != null) {
                        degreeGrantingInst = dto.getInstitucionOG().getDescripcion();
                    } else if (dto.getInstitucionOG().getIdDescripcion() != null) {
                        degreeGrantingInst = dto.getInstitucionOG().getIdDescripcion();
                    }
                }
                row.createCell(3).setCellValue(degreeGrantingInst);

                // Academic Degree (gradoAcademico)
                String academicDegree = "";
                if (dto.getGradoAcademico() != null) {
                    if (dto.getGradoAcademico().getDescripcion() != null) {
                        academicDegree = dto.getGradoAcademico().getDescripcion();
                    } else if (dto.getGradoAcademico().getIdDescripcion() != null) {
                        academicDegree = dto.getGradoAcademico().getIdDescripcion();
                    }
                }
                row.createCell(4).setCellValue(academicDegree);

                // Host Institution (institucion)
                String hostInst = "";
                if (dto.getInstitucion() != null) {
                    if (dto.getInstitucion().getDescripcion() != null) {
                        hostInst = dto.getInstitucion().getDescripcion();
                    } else if (dto.getInstitucion().getIdDescripcion() != null) {
                        hostInst = dto.getInstitucion().getIdDescripcion();
                    }
                }
                row.createCell(5).setCellValue(hostInst);

                // Thesis Status (estadoTesis.descripcion)
                String status = "";
                if (dto.getEstadoTesis() != null && dto.getEstadoTesis().getDescripcion() != null) {
                    status = dto.getEstadoTesis().getDescripcion();
                }
                row.createCell(6).setCellValue(status);

                // Program Start Date (fechaInicioPrograma)
                row.createCell(7).setCellValue(dto.getFechaInicioPrograma() != null ? dto.getFechaInicioPrograma() : "");

                // QE Date (fechaInicio)
                row.createCell(8).setCellValue(dto.getFechaInicio() != null ? dto.getFechaInicio() : "");

                // Degree Award Date (fechaTermino)
                row.createCell(9).setCellValue(dto.getFechaTermino() != null ? dto.getFechaTermino() : "");

                // Sector Types (descripciones separadas por coma)
                String sectorTypes = "";
                if (dto.getTipoSector() != null && !dto.getTipoSector().isEmpty()) {
                    String[] ids = dto.getTipoSector().split(",");
                    sectorTypes = java.util.Arrays.stream(ids)
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(s -> {
                            try {
                                Long id = Long.parseLong(s);
                                return sectorDescriptions.getOrDefault(id, "");
                            } catch (NumberFormatException e) {
                                return "";
                            }
                        })
                        .filter(s -> s != null && !s.isEmpty())
                        .collect(Collectors.joining(", "));
                }
                row.createCell(10).setCellValue(sectorTypes);

                // Clusters (números romanos separados por coma)
                String clusters = "";
                if (dto.getCluster() != null && !dto.getCluster().isEmpty()) {
                    clusters = java.util.Arrays.stream(dto.getCluster().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(s -> {
                            switch (s) {
                                case "1": return "I";
                                case "2": return "II";
                                case "3": return "III";
                                case "4": return "IV";
                                case "5": return "V";
                                default: return s;
                            }
                        })
                        .collect(Collectors.joining(", "));
                }
                row.createCell(11).setCellValue(clusters);

                // Basal (Si/No)
                String basal = "";
                if (dto.getBasal() != null) {
                    basal = (dto.getBasal().equalsIgnoreCase("S") || dto.getBasal().equals("1")) ? "Si" : "No";
                }
                row.createCell(12).setCellValue(basal);

                // Period (solo número)
                if (dto.getProgressReport() != null) {
                    row.createCell(13).setCellValue(dto.getProgressReport());
                } else {
                    row.createCell(13).setCellValue("");
                }

                // ANID Code
                row.createCell(14).setCellValue(dto.getCodigoANID() != null ? dto.getCodigoANID() : "");

                // --- Participantes por tipo de participación ---
                String principalNames = "";
                String principalGenders = "";
                String principalTypes = "";

                String supervisorNames = "";
                String supervisorGenders = "";
                String supervisorTypes = "";

                String studentNames = "";
                String studentGenders = "";
                String studentTypes = "";

                List<ParticipacionProducto> participaciones = participacionProductoRepository.findByProductoId(tesis.getId());
                for (ParticipacionProducto pp : participaciones) {
                    RRHH rrhh = pp.getRrhh();
                    TipoParticipacion tp = pp.getTipoParticipacion();
                    if (rrhh == null || tp == null) {
                        continue;
                    }

                    Long roleId = tp.getId();
                    String role = tp.getDescripcion() != null ? tp.getDescripcion().trim() : "";
                    String name = rrhh.getFullname() != null ? rrhh.getFullname() : "";
                    String gender = rrhh.getCodigoGenero() != null ? rrhh.getCodigoGenero() : "";
                    String tipoRRHH = "";
                    if (rrhh.getTipoRRHH() != null) {
                        if (rrhh.getTipoRRHH().getDescripcion() != null) {
                            tipoRRHH = rrhh.getTipoRRHH().getDescripcion();
                        } else if (rrhh.getTipoRRHH().getCodigoDescripcion() != null) {
                            tipoRRHH = rrhh.getTipoRRHH().getCodigoDescripcion();
                        }
                    }

                    // Clasificación robusta por ID (preferente) y por descripción (fallback)
                    boolean isPrincipalSupervisor =
                        (roleId != null && roleId == 12L) ||
                        role.equalsIgnoreCase("Principal Supervisor");

                    boolean isSupervisor =
                        (roleId != null && roleId == 13L) ||
                        role.equalsIgnoreCase("Supervisor");

                    boolean isStudent =
                        (roleId != null && roleId == 7L) ||
                        role.equalsIgnoreCase("Student") ||
                        role.equalsIgnoreCase("Estudiante");

                    if (isPrincipalSupervisor) {
                        principalNames = appendWithSeparator(principalNames, name);
                        principalGenders = appendWithSeparator(principalGenders, gender);
                        principalTypes = appendWithSeparator(principalTypes, tipoRRHH);
                    } else if (isSupervisor) {
                        supervisorNames = appendWithSeparator(supervisorNames, name);
                        supervisorGenders = appendWithSeparator(supervisorGenders, gender);
                        supervisorTypes = appendWithSeparator(supervisorTypes, tipoRRHH);
                    } else if (isStudent) {
                        studentNames = appendWithSeparator(studentNames, name);
                        studentGenders = appendWithSeparator(studentGenders, gender);
                        studentTypes = appendWithSeparator(studentTypes, tipoRRHH);
                    }
                }

                // Principal Supervisor columns
                row.createCell(15).setCellValue(principalNames);
                row.createCell(16).setCellValue(principalGenders);
                row.createCell(17).setCellValue(principalTypes);

                // Supervisor columns
                row.createCell(18).setCellValue(supervisorNames);
                row.createCell(19).setCellValue(supervisorGenders);
                row.createCell(20).setCellValue(supervisorTypes);

                // Student columns
                row.createCell(21).setCellValue(studentNames);
                row.createCell(22).setCellValue(studentGenders);
                row.createCell(23).setCellValue(studentTypes);
            }

            for (int i = 0; i <= 23; i++) {
                sheet.autoSizeColumn(i);
            }

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=thesis-students.xlsx");
            workbook.write(response.getOutputStream());
            workbook.close();
            response.flushBuffer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Obtiene una tesis específica por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<TesisDTO> getThesis(@PathVariable Long id) {
        Long idRRHH = userService.getCurrentUserIdRRHH().orElse(null);
        String userName = userService.getCurrentUsername().orElse(null);
        return tesisRepository.findVisibleByIdAndUserIdRRHH(id, idRRHH, userName)
            .map(tesis -> ResponseEntity.ok(convertToDTO(tesis)))
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Crea una nueva tesis
     */
    @PostMapping
    @Transactional
    public ResponseEntity<TesisDTO> createThesis(@RequestBody TesisDTO dto) {
        try {
            Tesis tesis = convertFromDTO(dto);
            // Establecer el username del usuario actual solo al crear
            userService.getCurrentUsername().ifPresent(tesis::setUsername);
            // Establecer tipoProducto por defecto (id = 11 para Tesis) si no viene en el DTO
            if (tesis.getTipoProducto() == null) {
                tipoProductoRepository.findById(11L)
                    .ifPresent(tesis::setTipoProducto);
            }
            Tesis saved = tesisRepository.save(tesis);

            if (dto.getParticipantes() != null && !dto.getParticipantes().isEmpty()) {
                saveParticipantes(saved, dto.getParticipantes());
            }

            TesisDTO resultDTO = convertToDTO(saved);
            return ResponseEntity.ok(resultDTO);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Actualiza una tesis existente
     */
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<TesisDTO> updateThesis(
            @PathVariable Long id,
            @RequestBody TesisDTO dto) {

        return tesisRepository.findById(id)
            .map(existingTesis -> {
                // Actualizar campos de ProductoCientifico
                // Actualizar descripción: si ya existe código, actualizar; si no, crear nuevo
                if (dto.getDescripcion() != null && !dto.getDescripcion().isEmpty()) {
                    if (existingTesis.getDescripcion() != null && !existingTesis.getDescripcion().isEmpty()) {
                        textosService.updateTextInBothLanguages(existingTesis.getDescripcion(), dto.getDescripcion(), 2);
                    } else {
                        String codigoDescripcion = textosService.createTextInBothLanguages(dto.getDescripcion(), 2);
                        existingTesis.setDescripcion(codigoDescripcion);
                    }
                }
                // Actualizar comentario: si ya existe código, actualizar; si no, crear nuevo
                if (dto.getComentario() != null && !dto.getComentario().isEmpty()) {
                    if (existingTesis.getComentario() != null && !existingTesis.getComentario().isEmpty()) {
                        textosService.updateTextInBothLanguages(existingTesis.getComentario(), dto.getComentario(), 2);
                    } else {
                        String codigoComentario = textosService.createTextInBothLanguages(dto.getComentario(), 2);
                        existingTesis.setComentario(codigoComentario);
                    }
                }
                // fechaInicio es obligatorio
                if (dto.getFechaInicio() == null || dto.getFechaInicio().isEmpty()) {
                    throw new IllegalArgumentException("fechaInicio is required");
                }
                existingTesis.setFechaInicio(parseLocalDate(dto.getFechaInicio()));
                if (dto.getFechaTermino() != null) {
                    existingTesis.setFechaTermino(parseLocalDate(dto.getFechaTermino()));
                }
                if (dto.getUrlDocumento() != null) {
                    existingTesis.setUrlDocumento(dto.getUrlDocumento());
                }
                if (dto.getLinkVisualizacion() != null) {
                    existingTesis.setLinkVisualizacion(dto.getLinkVisualizacion());
                }
                // Actualizar linkPDF siempre (puede ser null para limpiarlo)
                existingTesis.setLinkPDF(dto.getLinkPDF());
                existingTesis.setProgressReport(dto.getProgressReport());
                if (dto.getCodigoANID() != null) {
                    existingTesis.setCodigoANID(dto.getCodigoANID());
                }
                // Manejar basal: debe ser "S" o "N"
                if (dto.getBasal() != null && !dto.getBasal().isEmpty()) {
                    char basalValue = dto.getBasal().charAt(0);
                    if (basalValue == 'S' || basalValue == 's' || basalValue == 'N' || basalValue == 'n') {
                        existingTesis.setBasal(Character.toUpperCase(basalValue));
                    } else {
                        existingTesis.setBasal('N');
                    }
                } else {
                    existingTesis.setBasal('N');
                }
                if (dto.getLineasInvestigacion() != null) {
                    existingTesis.setLineasInvestigacion(dto.getLineasInvestigacion());
                }
                if (dto.getCluster() != null) {
                    existingTesis.setCluster(dto.getCluster());
                }

                // Actualizar relaciones de ProductoCientifico
                if (dto.getTipoProducto() != null && dto.getTipoProducto().getId() != null) {
                    tipoProductoRepository.findById(dto.getTipoProducto().getId())
                        .ifPresent(existingTesis::setTipoProducto);
                }
                if (dto.getEstadoProducto() != null && dto.getEstadoProducto().getId() != null) {
                    estadoProductoRepository.findById(dto.getEstadoProducto().getId())
                        .ifPresent(existingTesis::setEstadoProducto);
                }

                // Actualizar campos específicos de Tesis
                if (dto.getInstitucionOG() != null && dto.getInstitucionOG().getId() != null) {
                    institucionRepository.findById(dto.getInstitucionOG().getId())
                        .ifPresent(existingTesis::setInstitucionOG);
                }
                if (dto.getGradoAcademico() != null && dto.getGradoAcademico().getId() != null) {
                    gradoAcademicoRepository.findById(dto.getGradoAcademico().getId())
                        .ifPresent(existingTesis::setGradoAcademico);
                }
                if (dto.getInstitucion() != null && dto.getInstitucion().getId() != null) {
                    institucionRepository.findById(dto.getInstitucion().getId())
                        .ifPresent(existingTesis::setInstitucion);
                }
                if (dto.getEstadoTesis() != null && dto.getEstadoTesis().getId() != null) {
                    estadoTesisRepository.findById(dto.getEstadoTesis().getId())
                        .ifPresent(existingTesis::setEstadoTesis);
                }
                if (dto.getFechaInicioPrograma() != null) {
                    existingTesis.setFechaInicioPrograma(parseLocalDate(dto.getFechaInicioPrograma()));
                }
                if (dto.getNombreCompletoTitulo() != null) {
                    existingTesis.setNombreCompletoTitulo(dto.getNombreCompletoTitulo());
                }
                if (dto.getTipoSector() != null) {
                    existingTesis.setTipoSector(dto.getTipoSector());
                }

                // Actualizar participantes
                if (dto.getParticipantes() != null) {
                    participacionProductoRepository.deleteByProductoId(existingTesis.getId());
                    if (!dto.getParticipantes().isEmpty()) {
                        saveParticipantes(existingTesis, dto.getParticipantes());
                    }
                }

                Tesis updated = tesisRepository.save(existingTesis);
                return ResponseEntity.ok(convertToDTO(updated));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Elimina una tesis
     */
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deleteThesis(@PathVariable Long id) {
        return tesisRepository.findById(id)
            .map(tesis -> {
                // Eliminar archivo PDF asociado si existe
                if (tesis.getLinkPDF() != null && !tesis.getLinkPDF().trim().isEmpty()) {
                    pdfFileService.deletePdfFile(tesis.getLinkPDF());
                }
                // Eliminar participantes asociados
                participacionProductoRepository.deleteByProductoId(id);
                // Eliminar el registro
                tesisRepository.deleteById(id);
                return ResponseEntity.noContent().<Void>build();
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Convierte una entidad Tesis a DTO (sin participantes - para listas)
     */
    private TesisDTO convertToDTOWithoutParticipants(Tesis tesis) {
        return convertToDTOWithoutParticipants(tesis, null);
    }
    
    /**
     * Convierte una entidad Tesis a DTO (sin participantes - para listas) con textos precargados
     */
    private TesisDTO convertToDTOWithoutParticipants(Tesis tesis, Map<String, String> textosMap) {
        TesisDTO dto = convertToDTOBase(tesis, textosMap);
        // No cargar participantes para mejorar rendimiento en listas
        dto.setParticipantes(null);
        return dto;
    }

    /**
     * Convierte una entidad Tesis a DTO (con participantes - para detalles)
     */
    private TesisDTO convertToDTO(Tesis tesis) {
        TesisDTO dto = convertToDTOBase(tesis);
        
        // Cargar participantes solo cuando se necesita (detalles)
        List<ParticipacionProducto> participaciones = participacionProductoRepository.findByProductoId(tesis.getId());
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
     * Método base para convertir Tesis a DTO (sin participantes)
     */
    private TesisDTO convertToDTOBase(Tesis tesis) {
        return convertToDTOBase(tesis, null);
    }
    
    /**
     * Método base para convertir Tesis a DTO (sin participantes) con textos precargados
     */
    private TesisDTO convertToDTOBase(Tesis tesis, Map<String, String> textosMap) {
        TesisDTO dto = new TesisDTO();

        // Campos de ProductoCientifico
        dto.setId(tesis.getId());
        // Obtener el valor del texto desde la tabla textos usando el código guardado en idDescripcion
        if (tesis.getDescripcion() != null && !tesis.getDescripcion().isEmpty()) {
            String descripcion = textosMap != null && textosMap.containsKey(tesis.getDescripcion())
                ? textosMap.get(tesis.getDescripcion())
                : textosService.getTextValue(tesis.getDescripcion(), 2, "us").orElse(tesis.getDescripcion());
            dto.setDescripcion(descripcion);
        }
        // Obtener el valor del texto desde la tabla textos usando el código guardado en idComentario
        if (tesis.getComentario() != null && !tesis.getComentario().isEmpty()) {
            String comentario = textosMap != null && textosMap.containsKey(tesis.getComentario())
                ? textosMap.get(tesis.getComentario())
                : textosService.getTextValue(tesis.getComentario(), 2, "us").orElse(tesis.getComentario());
            dto.setComentario(comentario);
        }
        dto.setFechaInicio(tesis.getFechaInicio() != null ? tesis.getFechaInicio().toString() : null);
        dto.setFechaTermino(tesis.getFechaTermino() != null ? tesis.getFechaTermino().toString() : null);
        dto.setUrlDocumento(tesis.getUrlDocumento());
        dto.setLinkVisualizacion(tesis.getLinkVisualizacion());
        dto.setLinkPDF(tesis.getLinkPDF());
        dto.setProgressReport(tesis.getProgressReport());
        dto.setCodigoANID(tesis.getCodigoANID());
        // Normalizar basal: convertir '1' a 'S' y '0' a 'N', mantener 'S'/'N' como están
        if (tesis.getBasal() != null) {
            char basalChar = tesis.getBasal();
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
        dto.setLineasInvestigacion(tesis.getLineasInvestigacion());
        dto.setCluster(tesis.getCluster());
        dto.setParticipantesNombres(tesis.getParticipantesNombres());
        dto.setCreatedAt(tesis.getCreatedAt() != null ? tesis.getCreatedAt().toString() : null);
        dto.setUpdatedAt(tesis.getUpdatedAt() != null ? tesis.getUpdatedAt().toString() : null);

        // Relaciones de ProductoCientifico
        if (tesis.getTipoProducto() != null) {
            TipoProductoDTO tipoDTO = new TipoProductoDTO();
            tipoDTO.setId(tesis.getTipoProducto().getId());
            tipoDTO.setIdDescripcion(tesis.getTipoProducto().getIdDescripcion());
            tipoDTO.setDescripcion(tesis.getTipoProducto().getDescripcion());
            dto.setTipoProducto(tipoDTO);
        }

        if (tesis.getEstadoProducto() != null) {
            EstadoProductoDTO estadoDTO = new EstadoProductoDTO();
            estadoDTO.setId(tesis.getEstadoProducto().getId());
            estadoDTO.setCodigoDescripcion(tesis.getEstadoProducto().getCodigoDescripcion());
            estadoDTO.setCreatedAt(tesis.getEstadoProducto().getCreatedAt() != null ?
                tesis.getEstadoProducto().getCreatedAt().toString() : null);
            estadoDTO.setUpdatedAt(tesis.getEstadoProducto().getUpdatedAt() != null ?
                tesis.getEstadoProducto().getUpdatedAt().toString() : null);
            dto.setEstadoProducto(estadoDTO);
        }

        // Campos específicos de Tesis
        if (tesis.getInstitucionOG() != null) {
            InstitucionDTO institucionOGDTO = new InstitucionDTO();
            institucionOGDTO.setId(tesis.getInstitucionOG().getId());
            institucionOGDTO.setIdDescripcion(tesis.getInstitucionOG().getIdDescripcion());
            institucionOGDTO.setDescripcion(tesis.getInstitucionOG().getDescripcion());
            dto.setInstitucionOG(institucionOGDTO);
        }

        if (tesis.getGradoAcademico() != null) {
            GradoAcademicoDTO gradoDTO = new GradoAcademicoDTO();
            gradoDTO.setId(tesis.getGradoAcademico().getId());
            gradoDTO.setIdDescripcion(tesis.getGradoAcademico().getIdDescripcion());
            gradoDTO.setDescripcion(tesis.getGradoAcademico().getDescripcion());
            dto.setGradoAcademico(gradoDTO);
        }

        if (tesis.getInstitucion() != null) {
            InstitucionDTO institucionDTO = new InstitucionDTO();
            institucionDTO.setId(tesis.getInstitucion().getId());
            institucionDTO.setIdDescripcion(tesis.getInstitucion().getIdDescripcion());
            institucionDTO.setDescripcion(tesis.getInstitucion().getDescripcion());
            dto.setInstitucion(institucionDTO);
        }

        if (tesis.getEstadoTesis() != null) {
            EstadoTesisDTO estadoTesisDTO = new EstadoTesisDTO();
            estadoTesisDTO.setId(tesis.getEstadoTesis().getId());
            estadoTesisDTO.setIdDescripcion(tesis.getEstadoTesis().getIdDescripcion());
            estadoTesisDTO.setDescripcion(tesis.getEstadoTesis().getDescripcion());
            dto.setEstadoTesis(estadoTesisDTO);
        }

        dto.setFechaInicioPrograma(tesis.getFechaInicioPrograma() != null ? tesis.getFechaInicioPrograma().toString() : null);
        dto.setNombreCompletoTitulo(tesis.getNombreCompletoTitulo());
        dto.setTipoSector(tesis.getTipoSector());
        dto.setEstudiante(tesis.getEstudiante());

        return dto;
    }

    /**
     * Convierte un DTO a entidad Tesis
     */
    private Tesis convertFromDTO(TesisDTO dto) {
        Tesis tesis = new Tesis();

        // Campos de ProductoCientifico
        // Generar código de texto para descripción si se proporciona
        if (dto.getDescripcion() != null && !dto.getDescripcion().isEmpty()) {
            String codigoDescripcion = textosService.createTextInBothLanguages(dto.getDescripcion(), 2);
            tesis.setDescripcion(codigoDescripcion); // Guardar el código en idDescripcion
        }
        // Generar código de texto para comentario si se proporciona
        if (dto.getComentario() != null && !dto.getComentario().isEmpty()) {
            String codigoComentario = textosService.createTextInBothLanguages(dto.getComentario(), 2);
            tesis.setComentario(codigoComentario); // Guardar el código en idComentario
        }
        // fechaInicio es obligatorio
        if (dto.getFechaInicio() == null || dto.getFechaInicio().isEmpty()) {
            throw new IllegalArgumentException("fechaInicio is required");
        }
        tesis.setFechaInicio(parseLocalDate(dto.getFechaInicio()));
        if (dto.getFechaTermino() != null) {
            tesis.setFechaTermino(parseLocalDate(dto.getFechaTermino()));
        }
        tesis.setUrlDocumento(dto.getUrlDocumento());
        tesis.setLinkVisualizacion(dto.getLinkVisualizacion());
        tesis.setLinkPDF(dto.getLinkPDF());
        tesis.setProgressReport(dto.getProgressReport());
        tesis.setCodigoANID(dto.getCodigoANID());
        // Manejar basal: debe ser "S" o "N"
        if (dto.getBasal() != null && !dto.getBasal().isEmpty()) {
            char basalValue = dto.getBasal().charAt(0);
            if (basalValue == 'S' || basalValue == 's' || basalValue == 'N' || basalValue == 'n') {
                tesis.setBasal(Character.toUpperCase(basalValue));
            } else {
                tesis.setBasal('S');
            }
        } else {
            tesis.setBasal('S');
        }
        tesis.setLineasInvestigacion(dto.getLineasInvestigacion());
        tesis.setCluster(dto.getCluster());

        // Relaciones de ProductoCientifico
        if (dto.getTipoProducto() != null && dto.getTipoProducto().getId() != null) {
            tipoProductoRepository.findById(dto.getTipoProducto().getId())
                .ifPresent(tesis::setTipoProducto);
        }

        if (dto.getEstadoProducto() != null && dto.getEstadoProducto().getId() != null) {
            estadoProductoRepository.findById(dto.getEstadoProducto().getId())
                .ifPresent(tesis::setEstadoProducto);
        }

        // Campos específicos de Tesis
        if (dto.getInstitucionOG() != null && dto.getInstitucionOG().getId() != null) {
            institucionRepository.findById(dto.getInstitucionOG().getId())
                .ifPresent(tesis::setInstitucionOG);
        }
        if (dto.getGradoAcademico() != null && dto.getGradoAcademico().getId() != null) {
            gradoAcademicoRepository.findById(dto.getGradoAcademico().getId())
                .ifPresent(tesis::setGradoAcademico);
        }
        if (dto.getInstitucion() != null && dto.getInstitucion().getId() != null) {
            institucionRepository.findById(dto.getInstitucion().getId())
                .ifPresent(tesis::setInstitucion);
        }
        if (dto.getEstadoTesis() != null && dto.getEstadoTesis().getId() != null) {
            estadoTesisRepository.findById(dto.getEstadoTesis().getId())
                .ifPresent(tesis::setEstadoTesis);
        }
        if (dto.getFechaInicioPrograma() != null) {
            tesis.setFechaInicioPrograma(parseLocalDate(dto.getFechaInicioPrograma()));
        }
        tesis.setNombreCompletoTitulo(dto.getNombreCompletoTitulo());
        tesis.setTipoSector(dto.getTipoSector());

        return tesis;
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
                // Intentar con formato ISO
                return LocalDate.parse(dateStr, DateTimeFormatter.ISO_DATE);
            } catch (Exception e2) {
                return null;
            }
        }
    }

    /**
     * Guarda los participantes de una tesis
     */
    private void saveParticipantes(Tesis tesis, List<ParticipanteDTO> participantesDTO) {
        for (ParticipanteDTO pDTO : participantesDTO) {
            if (pDTO.getRrhhId() == null || pDTO.getTipoParticipacionId() == null) {
                continue; // Saltar si faltan datos requeridos
            }

            RRHH rrhh = rrhhRepository.findById(pDTO.getRrhhId()).orElse(null);
            TipoParticipacion tipoParticipacion = tipoParticipacionRepository.findById(pDTO.getTipoParticipacionId()).orElse(null);

            if (rrhh != null && tipoParticipacion != null) {
                // Obtener el siguiente ID correlativo para esta combinación (idProducto, idRRHH)
                Long nextId = participacionProductoRepository.getNextIdForParticipacion(
                    tesis.getId(), 
                    rrhh.getId()
                );
                
                ParticipacionProducto participacion = new ParticipacionProducto();
                participacion.setRrhh(rrhh);
                participacion.setProducto(tesis);
                participacion.setTipoParticipacion(tipoParticipacion);
                participacion.setOrden(pDTO.getOrden() != null ? pDTO.getOrden() : 0);
                participacion.setCorresponding(pDTO.getCorresponding() != null && pDTO.getCorresponding());

                // Establecer el ID compuesto con el correlativo
                ParticipacionProductoId id = new ParticipacionProductoId(
                    rrhh.getId(), 
                    tesis.getId(), 
                    nextId
                );
                participacion.setId(id);

                participacionProductoRepository.save(participacion);
            }
        }
    }

    /**
     * Helper para concatenar valores con separador "; "
     */
    private String appendWithSeparator(String existing, String toAdd) {
        if (toAdd == null || toAdd.trim().isEmpty()) {
            return existing != null ? existing : "";
        }
        if (existing == null || existing.isEmpty()) {
            return toAdd;
        }
        return existing + "; " + toAdd;
    }
}


