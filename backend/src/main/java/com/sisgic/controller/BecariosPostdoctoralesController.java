package com.sisgic.controller;

import com.sisgic.dto.BecariosPostdoctoralesDTO;
import com.sisgic.dto.EstadoProductoDTO;
import com.sisgic.dto.ParticipanteDTO;
import com.sisgic.dto.InstitucionDTO;
import com.sisgic.dto.TipoSectorDTO;
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
import java.util.stream.Collectors;
import java.util.HashMap;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/postdoctoral-fellows")
@CrossOrigin(origins = "*")
public class BecariosPostdoctoralesController {

    @Autowired
    private BecariosPostdoctoralesRepository becariosPostdoctoralesRepository;

    @Autowired
    private InstitucionRepository institucionRepository;

    @Autowired
    private TipoSectorRepository tipoSectorRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private FundingTypeRepository fundingTypeRepository;

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
    private TextosService textosService;

    @Autowired
    private com.sisgic.service.PdfFileService pdfFileService;

    @Autowired
    private com.sisgic.service.UserService userService;

    /**
     * Obtiene todos los becarios postdoctorales con paginación
     */
    @GetMapping
    public ResponseEntity<Page<BecariosPostdoctoralesDTO>> getPostdoctoralFellows(
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
        Page<BecariosPostdoctorales> fellows = becariosPostdoctoralesRepository.findVisibleByUserIdRRHH(idRRHH, userName, pageable);

        // Cargar todos los textos de una vez para optimizar consultas
        List<BecariosPostdoctorales> fellowsList = fellows.getContent();
        List<String> codigosTexto = new ArrayList<>();
        for (BecariosPostdoctorales f : fellowsList) {
            if (f.getDescripcion() != null && !f.getDescripcion().isEmpty()) {
                codigosTexto.add(f.getDescripcion());
            }
            if (f.getComentario() != null && !f.getComentario().isEmpty()) {
                codigosTexto.add(f.getComentario());
            }
        }
        Map<String, String> textosMap = textosService.getTextValuesBatch(codigosTexto, 2, "us");

        // Para listas, no cargar participantes para mejorar rendimiento
        Page<BecariosPostdoctoralesDTO> fellowsDTO = fellows.map(f -> convertToDTOWithoutParticipants(f, textosMap));

        return ResponseEntity.ok(fellowsDTO);
    }

    /**
     * Obtiene un becario postdoctoral específico por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<BecariosPostdoctoralesDTO> getPostdoctoralFellow(@PathVariable Long id) {
        Long idRRHH = userService.getCurrentUserIdRRHH().orElse(null);
        String userName = userService.getCurrentUsername().orElse(null);
        return becariosPostdoctoralesRepository.findVisibleByIdAndUserIdRRHH(id, idRRHH, userName)
            .map(fellow -> ResponseEntity.ok(convertToDTO(fellow)))
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Crea un nuevo becario postdoctoral
     */
    @PostMapping
    @Transactional
    public ResponseEntity<BecariosPostdoctoralesDTO> createPostdoctoralFellow(@RequestBody BecariosPostdoctoralesDTO dto) {
        try {
            BecariosPostdoctorales fellow = convertFromDTO(dto);
            // Establecer el username del usuario actual solo al crear
            userService.getCurrentUsername().ifPresent(fellow::setUsername);
            BecariosPostdoctorales saved = becariosPostdoctoralesRepository.save(fellow);

            if (dto.getParticipantes() != null && !dto.getParticipantes().isEmpty()) {
                saveParticipantes(saved, dto.getParticipantes());
            }

            BecariosPostdoctoralesDTO resultDTO = convertToDTO(saved);
            return ResponseEntity.ok(resultDTO);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Actualiza un becario postdoctoral existente
     */
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<BecariosPostdoctoralesDTO> updatePostdoctoralFellow(
            @PathVariable Long id,
            @RequestBody BecariosPostdoctoralesDTO dto) {

        return becariosPostdoctoralesRepository.findById(id)
            .map(existingFellow -> {
                // Actualizar campos de ProductoCientifico
                if (dto.getDescripcion() != null && !dto.getDescripcion().isEmpty()) {
                    if (existingFellow.getDescripcion() != null && !existingFellow.getDescripcion().isEmpty()) {
                        textosService.updateTextInBothLanguages(existingFellow.getDescripcion(), dto.getDescripcion(), 2);
                    } else {
                        String codigoDescripcion = textosService.createTextInBothLanguages(dto.getDescripcion(), 2);
                        existingFellow.setDescripcion(codigoDescripcion);
                    }
                }
                if (dto.getComentario() != null && !dto.getComentario().isEmpty()) {
                    if (existingFellow.getComentario() != null && !existingFellow.getComentario().isEmpty()) {
                        textosService.updateTextInBothLanguages(existingFellow.getComentario(), dto.getComentario(), 2);
                    } else {
                        String codigoComentario = textosService.createTextInBothLanguages(dto.getComentario(), 2);
                        existingFellow.setComentario(codigoComentario);
                    }
                }
                // fechaInicio es obligatorio
                if (dto.getFechaInicio() == null || dto.getFechaInicio().isEmpty()) {
                    throw new IllegalArgumentException("fechaInicio is required");
                }
                existingFellow.setFechaInicio(parseLocalDate(dto.getFechaInicio()));
                if (dto.getFechaTermino() != null) {
                    existingFellow.setFechaTermino(parseLocalDate(dto.getFechaTermino()));
                }
                if (dto.getUrlDocumento() != null) {
                    existingFellow.setUrlDocumento(dto.getUrlDocumento());
                }
                if (dto.getLinkVisualizacion() != null) {
                    existingFellow.setLinkVisualizacion(dto.getLinkVisualizacion());
                }
                // Actualizar linkPDF siempre (puede ser null para limpiarlo)
                existingFellow.setLinkPDF(dto.getLinkPDF());
                existingFellow.setProgressReport(dto.getProgressReport());
                if (dto.getCodigoANID() != null) {
                    existingFellow.setCodigoANID(dto.getCodigoANID());
                }
                if (dto.getBasal() != null && !dto.getBasal().isEmpty()) {
                    char basalValue = dto.getBasal().charAt(0);
                    if (basalValue == 'S' || basalValue == 's' || basalValue == 'N' || basalValue == 'n') {
                        existingFellow.setBasal(Character.toUpperCase(basalValue));
                    } else {
                        existingFellow.setBasal('N');
                    }
                } else {
                    existingFellow.setBasal('N');
                }
                if (dto.getLineasInvestigacion() != null) {
                    existingFellow.setLineasInvestigacion(dto.getLineasInvestigacion());
                }
                if (dto.getCluster() != null) {
                    existingFellow.setCluster(dto.getCluster());
                }

                // Actualizar relaciones de ProductoCientifico
                if (dto.getTipoProducto() != null && dto.getTipoProducto().getId() != null) {
                    tipoProductoRepository.findById(dto.getTipoProducto().getId())
                        .ifPresent(existingFellow::setTipoProducto);
                }
                if (dto.getEstadoProducto() != null && dto.getEstadoProducto().getId() != null) {
                    estadoProductoRepository.findById(dto.getEstadoProducto().getId())
                        .ifPresent(existingFellow::setEstadoProducto);
                }

                // Actualizar campos específicos de BecariosPostdoctorales
                if (dto.getInstitucion() != null && dto.getInstitucion().getId() != null) {
                    institucionRepository.findById(dto.getInstitucion().getId())
                        .ifPresent(existingFellow::setInstitucion);
                }
                if (dto.getFundingSource() != null) {
                    existingFellow.setFundingSource(dto.getFundingSource());
                }
                if (dto.getTipoSector() != null && dto.getTipoSector().getId() != null) {
                    tipoSectorRepository.findById(dto.getTipoSector().getId())
                        .ifPresent(existingFellow::setTipoSector);
                }
                if (dto.getResources() != null) {
                    existingFellow.setResources(dto.getResources());
                }

                // Actualizar participantes
                if (dto.getParticipantes() != null) {
                    participacionProductoRepository.deleteByProductoId(existingFellow.getId());
                    if (!dto.getParticipantes().isEmpty()) {
                        saveParticipantes(existingFellow, dto.getParticipantes());
                    }
                }

                BecariosPostdoctorales updated = becariosPostdoctoralesRepository.save(existingFellow);
                return ResponseEntity.ok(convertToDTO(updated));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Elimina un becario postdoctoral
     */
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deletePostdoctoralFellow(@PathVariable Long id) {
        return becariosPostdoctoralesRepository.findById(id)
            .map(fellow -> {
                // Eliminar archivo PDF asociado si existe
                if (fellow.getLinkPDF() != null && !fellow.getLinkPDF().trim().isEmpty()) {
                    pdfFileService.deletePdfFile(fellow.getLinkPDF());
                }
                // Eliminar participantes asociados
                participacionProductoRepository.deleteByProductoId(id);
                // Eliminar el registro
                becariosPostdoctoralesRepository.deleteById(id);
                return ResponseEntity.noContent().<Void>build();
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Exporta los becarios postdoctorales visibles a Excel
     */
    @GetMapping("/export")
    @Transactional(readOnly = true)
    public void exportPostdoctoralFellowsToExcel(
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            HttpServletResponse response) {

        try {
            Sort sort = sortDir.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() :
                Sort.by(sortBy).ascending();

            Long idRRHH = userService.getCurrentUserIdRRHH().orElse(null);
            String userName = userService.getCurrentUsername().orElse(null);
            List<BecariosPostdoctorales> fellowsList = becariosPostdoctoralesRepository
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

            // Mapear resources (id -> descripción)
            Map<Long, String> resourceDescriptions = new HashMap<>();
            resourceRepository.findAll().forEach(r -> {
                if (r.getId() != null && r.getIdDescripcion() != null) {
                    String desc = textosService
                        .getTextValue(r.getIdDescripcion(), 2, "us")
                        .orElse(r.getIdDescripcion());
                    resourceDescriptions.put(r.getId(), desc);
                }
            });

            // Mapear funding types (id -> descripción)
            Map<Long, String> fundingDescriptions = new HashMap<>();
            fundingTypeRepository.findAll().forEach(f -> {
                if (f.getId() != null && f.getIdDescripcion() != null) {
                    String desc = textosService
                        .getTextValue(f.getIdDescripcion(), 2, "us")
                        .orElse(f.getIdDescripcion());
                    fundingDescriptions.put(f.getId(), desc);
                }
            });

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Postdoctoral Fellows");

            int rowIdx = 0;
            Row header = sheet.createRow(rowIdx++);
            header.createCell(0).setCellValue("ID");
            header.createCell(1).setCellValue("Research Topic");
            header.createCell(2).setCellValue("Institution where it was inserted");
            header.createCell(3).setCellValue("Inserted");
            header.createCell(4).setCellValue("Start Date");
            header.createCell(5).setCellValue("End Date");
            header.createCell(6).setCellValue("Funding Source");
            header.createCell(7).setCellValue("Resources");
            header.createCell(8).setCellValue("Clusters");
            header.createCell(9).setCellValue("Basal");
            header.createCell(10).setCellValue("Period");
            header.createCell(11).setCellValue("ANID Code");
            header.createCell(12).setCellValue("Created At");
            header.createCell(13).setCellValue("Principal Supervisor Name");
            header.createCell(14).setCellValue("Principal Supervisor Gender");
            header.createCell(15).setCellValue("Principal Supervisor Type");
            header.createCell(16).setCellValue("Supervisor Name");
            header.createCell(17).setCellValue("Supervisor Gender");
            header.createCell(18).setCellValue("Supervisor Type");
            header.createCell(19).setCellValue("Postdoctoral Fellow Name");
            header.createCell(20).setCellValue("Postdoctoral Fellow Gender");
            header.createCell(21).setCellValue("Postdoctoral Fellow Type");

            for (BecariosPostdoctorales fellow : fellowsList) {
                BecariosPostdoctoralesDTO dto = convertToDTOBase(fellow);
                Row row = sheet.createRow(rowIdx++);

                // ID (id interno)
                row.createCell(0).setCellValue(dto.getId() != null ? dto.getId() : 0);

                // Research Topic (descripcion traducida)
                row.createCell(1).setCellValue(dto.getDescripcion() != null ? dto.getDescripcion() : "");

                // Institution where it was inserted
                String institution = "";
                if (dto.getInstitucion() != null) {
                    if (dto.getInstitucion().getDescripcion() != null) {
                        institution = dto.getInstitucion().getDescripcion();
                    } else if (dto.getInstitucion().getIdDescripcion() != null) {
                        institution = dto.getInstitucion().getIdDescripcion();
                    }
                }
                row.createCell(2).setCellValue(institution);

                // Inserted (tipoSector)
                String sector = "";
                if (dto.getTipoSector() != null && dto.getTipoSector().getId() != null) {
                    sector = sectorDescriptions.getOrDefault(dto.getTipoSector().getId(), "");
                }
                row.createCell(3).setCellValue(sector);

                // Start Date / End Date
                row.createCell(4).setCellValue(dto.getFechaInicio() != null ? dto.getFechaInicio() : "");
                row.createCell(5).setCellValue(dto.getFechaTermino() != null ? dto.getFechaTermino() : "");

                // Funding Source (descripciones, incluyendo texto libre para "Other")
                String fundingLabel = "";
                if (dto.getFundingSource() != null && !dto.getFundingSource().trim().isEmpty()) {
                    String raw = dto.getFundingSource().trim();
                    try {
                        if (raw.startsWith("[")) {
                            // Formato JSON [{"id":7,"text":"Volkswagen"},{"id":1}]
                            List<String> labels = new java.util.ArrayList<>();
                            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                            java.util.List<?> arr = mapper.readValue(raw, java.util.List.class);
                            for (Object obj : arr) {
                                if (obj instanceof java.util.Map) {
                                    java.util.Map<?,?> m = (java.util.Map<?,?>) obj;
                                    Object idObj = m.get("id");
                                    Object textObj = m.get("text");
                                    if (idObj instanceof Number) {
                                        long id = ((Number) idObj).longValue();
                                        String base = fundingDescriptions.getOrDefault(id, String.valueOf(id));
                                        if (textObj != null && !textObj.toString().trim().isEmpty()) {
                                            labels.add(base + " (" + textObj.toString().trim() + ")");
                                        } else {
                                            labels.add(base);
                                        }
                                    }
                                }
                            }
                            fundingLabel = String.join(", ", labels);
                        } else {
                            // Formato antiguo: "1,2,3"
                            fundingLabel = java.util.Arrays.stream(raw.split(","))
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .map(s -> {
                                    try {
                                        Long id = Long.parseLong(s);
                                        return fundingDescriptions.getOrDefault(id, s);
                                    } catch (NumberFormatException e) {
                                        return s;
                                    }
                                })
                                .collect(Collectors.joining(", "));
                        }
                    } catch (Exception e) {
                        fundingLabel = raw;
                    }
                }
                row.createCell(6).setCellValue(fundingLabel);

                // Resources (descripciones)
                String resourcesLabel = "";
                if (dto.getResources() != null && !dto.getResources().trim().isEmpty()) {
                    String rawRes = dto.getResources().trim();
                    resourcesLabel = java.util.Arrays.stream(rawRes.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(s -> {
                            try {
                                Long id = Long.parseLong(s);
                                return resourceDescriptions.getOrDefault(id, s);
                            } catch (NumberFormatException e) {
                                return s;
                            }
                        })
                        .collect(Collectors.joining(", "));
                }
                row.createCell(7).setCellValue(resourcesLabel);

                // Clusters (I-V)
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
                row.createCell(8).setCellValue(clusters);

                // Basal (Si/No)
                String basal = "";
                if (dto.getBasal() != null) {
                    basal = (dto.getBasal().equalsIgnoreCase("S") || dto.getBasal().equals("1")) ? "Si" : "No";
                }
                row.createCell(9).setCellValue(basal);

                // Period
                row.createCell(10).setCellValue(dto.getProgressReport() != null ? dto.getProgressReport() : "");

                // ANID Code
                row.createCell(11).setCellValue(dto.getCodigoANID() != null ? dto.getCodigoANID() : "");

                // Created_at
                row.createCell(12).setCellValue(dto.getCreatedAt() != null ? dto.getCreatedAt() : "");

                // --- Participantes por tipo de participación ---
                String principalNames = "";
                String principalGenders = "";
                String principalTypes = "";

                String supervisorNames = "";
                String supervisorGenders = "";
                String supervisorTypes = "";

                String fellowNames = "";
                String fellowGenders = "";
                String fellowTypes = "";

                List<ParticipacionProducto> participaciones = participacionProductoRepository.findByProductoId(fellow.getId());
                for (ParticipacionProducto pp : participaciones) {
                    RRHH rrhh = pp.getRrhh();
                    TipoParticipacion tp = pp.getTipoParticipacion();
                    if (rrhh == null || tp == null) {
                        continue;
                    }

                    Long roleId = tp.getId();
                    String roleDesc = tp.getDescripcion() != null ? tp.getDescripcion().trim() : "";

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

                    boolean isPrincipalSupervisor =
                        (roleId != null && roleId == 17L) ||
                        roleDesc.equalsIgnoreCase("Principal Supervisor");

                    boolean isSupervisor =
                        (roleId != null && roleId == 18L) ||
                        roleDesc.equalsIgnoreCase("Supervisor");

                    boolean isFellow =
                        (roleId != null && roleId == 19L) ||
                        roleDesc.equalsIgnoreCase("Postdoctoral Fellow");

                    if (isPrincipalSupervisor) {
                        principalNames = appendWithSeparator(principalNames, name);
                        principalGenders = appendWithSeparator(principalGenders, gender);
                        principalTypes = appendWithSeparator(principalTypes, tipoRRHH);
                    } else if (isSupervisor) {
                        supervisorNames = appendWithSeparator(supervisorNames, name);
                        supervisorGenders = appendWithSeparator(supervisorGenders, gender);
                        supervisorTypes = appendWithSeparator(supervisorTypes, tipoRRHH);
                    } else if (isFellow) {
                        fellowNames = appendWithSeparator(fellowNames, name);
                        fellowGenders = appendWithSeparator(fellowGenders, gender);
                        fellowTypes = appendWithSeparator(fellowTypes, tipoRRHH);
                    }
                }

                // Principal Supervisor columns
                row.createCell(13).setCellValue(principalNames);
                row.createCell(14).setCellValue(principalGenders);
                row.createCell(15).setCellValue(principalTypes);

                // Supervisor columns
                row.createCell(16).setCellValue(supervisorNames);
                row.createCell(17).setCellValue(supervisorGenders);
                row.createCell(18).setCellValue(supervisorTypes);

                // Postdoctoral Fellow columns
                row.createCell(19).setCellValue(fellowNames);
                row.createCell(20).setCellValue(fellowGenders);
                row.createCell(21).setCellValue(fellowTypes);
            }

            for (int i = 0; i <= 21; i++) {
                sheet.autoSizeColumn(i);
            }

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=postdoctoral-fellows.xlsx");
            workbook.write(response.getOutputStream());
            workbook.close();
            response.flushBuffer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Convierte una entidad BecariosPostdoctorales a DTO (sin participantes - para listas)
     */
    private BecariosPostdoctoralesDTO convertToDTOWithoutParticipants(BecariosPostdoctorales fellow) {
        return convertToDTOWithoutParticipants(fellow, null);
    }
    
    /**
     * Convierte una entidad BecariosPostdoctorales a DTO (sin participantes - para listas) con textos precargados
     */
    private BecariosPostdoctoralesDTO convertToDTOWithoutParticipants(BecariosPostdoctorales fellow, Map<String, String> textosMap) {
        BecariosPostdoctoralesDTO dto = convertToDTOBase(fellow, textosMap);
        // No cargar participantes para mejorar rendimiento en listas
        dto.setParticipantes(null);
        return dto;
    }

    /**
     * Convierte una entidad BecariosPostdoctorales a DTO (con participantes - para detalles)
     */
    private BecariosPostdoctoralesDTO convertToDTO(BecariosPostdoctorales fellow) {
        BecariosPostdoctoralesDTO dto = convertToDTOBase(fellow);
        
        // Cargar participantes solo cuando se necesita (detalles)
        List<ParticipacionProducto> participaciones = participacionProductoRepository.findByProductoId(fellow.getId());
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
     * Método base para convertir BecariosPostdoctorales a DTO (sin participantes)
     */
    private BecariosPostdoctoralesDTO convertToDTOBase(BecariosPostdoctorales fellow) {
        return convertToDTOBase(fellow, null);
    }
    
    /**
     * Método base para convertir BecariosPostdoctorales a DTO (sin participantes) con textos precargados
     */
    private BecariosPostdoctoralesDTO convertToDTOBase(BecariosPostdoctorales fellow, Map<String, String> textosMap) {
        BecariosPostdoctoralesDTO dto = new BecariosPostdoctoralesDTO();

        // Campos de ProductoCientifico
        dto.setId(fellow.getId());
        if (fellow.getDescripcion() != null && !fellow.getDescripcion().isEmpty()) {
            String descripcion = textosMap != null && textosMap.containsKey(fellow.getDescripcion())
                ? textosMap.get(fellow.getDescripcion())
                : textosService.getTextValue(fellow.getDescripcion(), 2, "us").orElse(fellow.getDescripcion());
            dto.setDescripcion(descripcion);
        }
        if (fellow.getComentario() != null && !fellow.getComentario().isEmpty()) {
            String comentario = textosMap != null && textosMap.containsKey(fellow.getComentario())
                ? textosMap.get(fellow.getComentario())
                : textosService.getTextValue(fellow.getComentario(), 2, "us").orElse(fellow.getComentario());
            dto.setComentario(comentario);
        }
        dto.setFechaInicio(fellow.getFechaInicio() != null ? fellow.getFechaInicio().toString() : null);
        dto.setFechaTermino(fellow.getFechaTermino() != null ? fellow.getFechaTermino().toString() : null);
        dto.setUrlDocumento(fellow.getUrlDocumento());
        dto.setLinkVisualizacion(fellow.getLinkVisualizacion());
        dto.setLinkPDF(fellow.getLinkPDF());
        dto.setProgressReport(fellow.getProgressReport());
        dto.setCodigoANID(fellow.getCodigoANID());
        // Normalizar basal: convertir '1' a 'S' y '0' a 'N', mantener 'S'/'N' como están
        if (fellow.getBasal() != null) {
            char basalChar = fellow.getBasal();
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
        dto.setLineasInvestigacion(fellow.getLineasInvestigacion());
        dto.setCluster(fellow.getCluster());
        dto.setParticipantesNombres(fellow.getParticipantesNombres());
        dto.setCreatedAt(fellow.getCreatedAt() != null ? fellow.getCreatedAt().toString() : null);
        dto.setUpdatedAt(fellow.getUpdatedAt() != null ? fellow.getUpdatedAt().toString() : null);

        // Relaciones de ProductoCientifico
        if (fellow.getTipoProducto() != null) {
            TipoProductoDTO tipoDTO = new TipoProductoDTO();
            tipoDTO.setId(fellow.getTipoProducto().getId());
            tipoDTO.setIdDescripcion(fellow.getTipoProducto().getIdDescripcion());
            tipoDTO.setDescripcion(fellow.getTipoProducto().getDescripcion());
            dto.setTipoProducto(tipoDTO);
        }

        if (fellow.getEstadoProducto() != null) {
            EstadoProductoDTO estadoDTO = new EstadoProductoDTO();
            estadoDTO.setId(fellow.getEstadoProducto().getId());
            estadoDTO.setCodigoDescripcion(fellow.getEstadoProducto().getCodigoDescripcion());
            estadoDTO.setCreatedAt(fellow.getEstadoProducto().getCreatedAt() != null ?
                fellow.getEstadoProducto().getCreatedAt().toString() : null);
            estadoDTO.setUpdatedAt(fellow.getEstadoProducto().getUpdatedAt() != null ?
                fellow.getEstadoProducto().getUpdatedAt().toString() : null);
            dto.setEstadoProducto(estadoDTO);
        }

        // Campos específicos de BecariosPostdoctorales
        if (fellow.getInstitucion() != null) {
            InstitucionDTO institucionDTO = new InstitucionDTO();
            institucionDTO.setId(fellow.getInstitucion().getId());
            institucionDTO.setIdDescripcion(fellow.getInstitucion().getIdDescripcion());
            institucionDTO.setDescripcion(fellow.getInstitucion().getDescripcion());
            dto.setInstitucion(institucionDTO);
        }

        dto.setFundingSource(fellow.getFundingSource());

        if (fellow.getTipoSector() != null) {
            TipoSectorDTO tipoSectorDTO = new TipoSectorDTO();
            tipoSectorDTO.setId(fellow.getTipoSector().getId());
            tipoSectorDTO.setIdDescripcion(fellow.getTipoSector().getIdDescripcion());
            dto.setTipoSector(tipoSectorDTO);
        }

        dto.setResources(fellow.getResources());
        dto.setPostdoctoralFellowName(fellow.getPostdoctoralFellowName());

        return dto;
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

    /**
     * Convierte un DTO a entidad BecariosPostdoctorales
     */
    private BecariosPostdoctorales convertFromDTO(BecariosPostdoctoralesDTO dto) {
        BecariosPostdoctorales fellow = new BecariosPostdoctorales();

        // Campos de ProductoCientifico
        if (dto.getDescripcion() != null && !dto.getDescripcion().isEmpty()) {
            String codigoDescripcion = textosService.createTextInBothLanguages(dto.getDescripcion(), 2);
            fellow.setDescripcion(codigoDescripcion);
        }
        if (dto.getComentario() != null && !dto.getComentario().isEmpty()) {
            String codigoComentario = textosService.createTextInBothLanguages(dto.getComentario(), 2);
            fellow.setComentario(codigoComentario);
        }
        // fechaInicio es obligatorio
        if (dto.getFechaInicio() == null || dto.getFechaInicio().isEmpty()) {
            throw new IllegalArgumentException("fechaInicio is required");
        }
        fellow.setFechaInicio(parseLocalDate(dto.getFechaInicio()));
        if (dto.getFechaTermino() != null) {
            fellow.setFechaTermino(parseLocalDate(dto.getFechaTermino()));
        }
        fellow.setUrlDocumento(dto.getUrlDocumento());
        fellow.setLinkVisualizacion(dto.getLinkVisualizacion());
        fellow.setLinkPDF(dto.getLinkPDF());
        fellow.setProgressReport(dto.getProgressReport());
        fellow.setCodigoANID(dto.getCodigoANID());
        if (dto.getBasal() != null && !dto.getBasal().isEmpty()) {
            char basalValue = dto.getBasal().charAt(0);
            if (basalValue == 'S' || basalValue == 's' || basalValue == 'N' || basalValue == 'n') {
                fellow.setBasal(Character.toUpperCase(basalValue));
            } else {
                fellow.setBasal('S');
            }
        } else {
            fellow.setBasal('S');
        }
        fellow.setLineasInvestigacion(dto.getLineasInvestigacion());
        fellow.setCluster(dto.getCluster());

        // Relaciones de ProductoCientifico
        if (dto.getTipoProducto() != null && dto.getTipoProducto().getId() != null) {
            tipoProductoRepository.findById(dto.getTipoProducto().getId())
                .ifPresent(fellow::setTipoProducto);
        }

        if (dto.getEstadoProducto() != null && dto.getEstadoProducto().getId() != null) {
            estadoProductoRepository.findById(dto.getEstadoProducto().getId())
                .ifPresent(fellow::setEstadoProducto);
        }

        // Campos específicos de BecariosPostdoctorales
        if (dto.getInstitucion() != null && dto.getInstitucion().getId() != null) {
            institucionRepository.findById(dto.getInstitucion().getId())
                .ifPresent(fellow::setInstitucion);
        }
        fellow.setFundingSource(dto.getFundingSource());
        if (dto.getTipoSector() != null && dto.getTipoSector().getId() != null) {
            tipoSectorRepository.findById(dto.getTipoSector().getId())
                .ifPresent(fellow::setTipoSector);
        }
        fellow.setResources(dto.getResources());

        return fellow;
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

    /**
     * Guarda los participantes de un becario postdoctoral
     */
    private void saveParticipantes(BecariosPostdoctorales fellow, List<ParticipanteDTO> participantesDTO) {
        for (ParticipanteDTO pDTO : participantesDTO) {
            if (pDTO.getRrhhId() == null || pDTO.getTipoParticipacionId() == null) {
                continue;
            }

            RRHH rrhh = rrhhRepository.findById(pDTO.getRrhhId()).orElse(null);
            TipoParticipacion tipoParticipacion = tipoParticipacionRepository.findById(pDTO.getTipoParticipacionId()).orElse(null);

            if (rrhh != null && tipoParticipacion != null) {
                // Obtener el siguiente ID correlativo para esta combinación (idProducto, idRRHH)
                Long nextId = participacionProductoRepository.getNextIdForParticipacion(
                    fellow.getId(), 
                    rrhh.getId()
                );
                
                ParticipacionProducto participacion = new ParticipacionProducto();
                participacion.setRrhh(rrhh);
                participacion.setProducto(fellow);
                participacion.setTipoParticipacion(tipoParticipacion);
                participacion.setOrden(pDTO.getOrden() != null ? pDTO.getOrden() : 0);
                participacion.setCorresponding(pDTO.getCorresponding() != null && pDTO.getCorresponding());

                // Establecer el ID compuesto con el correlativo
                ParticipacionProductoId id = new ParticipacionProductoId(
                    rrhh.getId(), 
                    fellow.getId(), 
                    nextId
                );
                participacion.setId(id);

                participacionProductoRepository.save(participacion);
            }
        }
    }
}


