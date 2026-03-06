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
                if (dto.getProgressReport() != null) {
                    existingFellow.setProgressReport(dto.getProgressReport());
                }
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


