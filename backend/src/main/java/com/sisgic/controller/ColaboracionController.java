package com.sisgic.controller;

import com.sisgic.dto.*;
import com.sisgic.entity.*;
import com.sisgic.repository.*;
import com.sisgic.service.TextosService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/scientific-collaborations")
@CrossOrigin(origins = "*")
public class ColaboracionController {

    @Autowired
    private ColaboracionRepository colaboracionRepository;

    @Autowired
    private TipoColaboracionRepository tipoColaboracionRepository;

    @Autowired
    private InstitucionRepository institucionRepository;

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
    private TextosService textosService;

    @Autowired
    private com.sisgic.service.PdfFileService pdfFileService;

    @Autowired
    private com.sisgic.service.UserService userService;

    /**
     * Obtiene todas las colaboraciones científicas con paginación
     */
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<Page<ColaboracionDTO>> getScientificCollaborations(
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
        Page<Colaboracion> colaboraciones = colaboracionRepository.findVisibleByUserIdRRHH(idRRHH, userName, pageable);

        // Cargar relaciones lazy antes de convertir a DTO
        // Recargar cada entidad con sus relaciones usando findByIdWithRelations
        List<Colaboracion> colaboracionesList = colaboraciones.getContent();
        List<Colaboracion> colaboracionesConRelaciones = new ArrayList<>();
        for (Colaboracion c : colaboracionesList) {
            colaboracionRepository.findByIdWithRelations(c.getId())
                .ifPresent(colaboracionesConRelaciones::add);
        }

        // Cargar todos los textos de una vez para optimizar consultas
        List<String> codigosTexto = new ArrayList<>();
        for (Colaboracion c : colaboracionesConRelaciones) {
            if (c.getDescripcion() != null && !c.getDescripcion().isEmpty()) {
                codigosTexto.add(c.getDescripcion());
            }
            if (c.getComentario() != null && !c.getComentario().isEmpty()) {
                codigosTexto.add(c.getComentario());
            }
        }
        Map<String, String> textosMap = textosService.getTextValuesBatch(codigosTexto, 2, "us");

        // Convertir a DTO usando las entidades con relaciones cargadas
        List<ColaboracionDTO> colaboracionesDTOList = colaboracionesConRelaciones.stream()
            .map(c -> convertToDTOWithoutParticipants(c, textosMap))
            .collect(Collectors.toList());
        
        // Crear una nueva página con los DTOs convertidos
        Page<ColaboracionDTO> colaboracionesDTO = new PageImpl<>(
            colaboracionesDTOList,
            pageable,
            colaboraciones.getTotalElements()
        );

        return ResponseEntity.ok(colaboracionesDTO);
    }

    /**
     * Obtiene una colaboración científica específica por ID
     */
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<ColaboracionDTO> getScientificCollaboration(@PathVariable Long id) {
        Long idRRHH = userService.getCurrentUserIdRRHH().orElse(null);
        String userName = userService.getCurrentUsername().orElse(null);
        
        // Primero verificar visibilidad con la consulta nativa
        Optional<Colaboracion> colaboracionVisible = colaboracionRepository.findVisibleByIdAndUserIdRRHH(id, idRRHH, userName);
        if (colaboracionVisible.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        // Recargar la entidad con todas sus relaciones usando findByIdWithRelations
        return colaboracionRepository.findByIdWithRelations(id)
            .map(colaboracion -> ResponseEntity.ok(convertToDTO(colaboracion)))
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Crea una nueva colaboración científica
     */
    @PostMapping
    @Transactional
    public ResponseEntity<ColaboracionDTO> createScientificCollaboration(@RequestBody ColaboracionDTO dto) {
        try {
            Colaboracion colaboracion = convertFromDTO(dto);
            // Establecer el username del usuario actual solo al crear
            userService.getCurrentUsername().ifPresent(colaboracion::setUsername);
            Colaboracion saved = colaboracionRepository.save(colaboracion);

            if (dto.getParticipantes() != null && !dto.getParticipantes().isEmpty()) {
                saveParticipantes(saved, dto.getParticipantes());
            }

            // Recargar la entidad con todas sus relaciones antes de convertir a DTO
            Colaboracion colaboracionConRelaciones = colaboracionRepository.findByIdWithRelations(saved.getId())
                .orElse(saved);
            
            ColaboracionDTO resultDTO = convertToDTO(colaboracionConRelaciones);
            return ResponseEntity.ok(resultDTO);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Actualiza una colaboración científica existente
     */
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<ColaboracionDTO> updateScientificCollaboration(
            @PathVariable Long id,
            @RequestBody ColaboracionDTO dto) {

        return colaboracionRepository.findById(id)
            .map(existingColaboracion -> {
                // Actualizar campos de ProductoCientifico
                if (dto.getDescripcion() != null && !dto.getDescripcion().isEmpty()) {
                    if (existingColaboracion.getDescripcion() != null && !existingColaboracion.getDescripcion().isEmpty()) {
                        textosService.updateTextInBothLanguages(existingColaboracion.getDescripcion(), dto.getDescripcion(), 2);
                    } else {
                        String codigoDescripcion = textosService.createTextInBothLanguages(dto.getDescripcion(), 2);
                        existingColaboracion.setDescripcion(codigoDescripcion);
                    }
                }
                if (dto.getComentario() != null && !dto.getComentario().isEmpty()) {
                    if (existingColaboracion.getComentario() != null && !existingColaboracion.getComentario().isEmpty()) {
                        textosService.updateTextInBothLanguages(existingColaboracion.getComentario(), dto.getComentario(), 2);
                    } else {
                        String codigoComentario = textosService.createTextInBothLanguages(dto.getComentario(), 2);
                        existingColaboracion.setComentario(codigoComentario);
                    }
                }
                // fechaInicio es obligatorio
                if (dto.getFechaInicio() == null || dto.getFechaInicio().isEmpty()) {
                    throw new IllegalArgumentException("fechaInicio is required");
                }
                existingColaboracion.setFechaInicio(parseLocalDate(dto.getFechaInicio()));
                if (dto.getFechaTermino() != null) {
                    existingColaboracion.setFechaTermino(parseLocalDate(dto.getFechaTermino()));
                }
                if (dto.getUrlDocumento() != null) {
                    existingColaboracion.setUrlDocumento(dto.getUrlDocumento());
                }
                if (dto.getLinkVisualizacion() != null) {
                    existingColaboracion.setLinkVisualizacion(dto.getLinkVisualizacion());
                }
                // Actualizar linkPDF siempre (puede ser null para limpiarlo)
                existingColaboracion.setLinkPDF(dto.getLinkPDF());
                existingColaboracion.setProgressReport(dto.getProgressReport());
                if (dto.getCodigoANID() != null) {
                    existingColaboracion.setCodigoANID(dto.getCodigoANID());
                }
                if (dto.getBasal() != null && !dto.getBasal().isEmpty()) {
                    char basalValue = dto.getBasal().charAt(0);
                    if (basalValue == 'S' || basalValue == 's' || basalValue == 'N' || basalValue == 'n') {
                        existingColaboracion.setBasal(Character.toUpperCase(basalValue));
                    } else {
                        existingColaboracion.setBasal('N');
                    }
                } else {
                    existingColaboracion.setBasal('N');
                }
                if (dto.getLineasInvestigacion() != null) {
                    existingColaboracion.setLineasInvestigacion(dto.getLineasInvestigacion());
                }
                if (dto.getCluster() != null) {
                    existingColaboracion.setCluster(dto.getCluster());
                }

                // Actualizar campos específicos de Colaboracion
                if (dto.getTipoColaboracion() != null && dto.getTipoColaboracion().getId() != null) {
                    tipoColaboracionRepository.findById(dto.getTipoColaboracion().getId())
                        .ifPresent(existingColaboracion::setTipoColaboracion);
                }
                if (dto.getInstitucion() != null && dto.getInstitucion().getId() != null) {
                    institucionRepository.findById(dto.getInstitucion().getId())
                        .ifPresent(existingColaboracion::setInstitucion);
                }
                if (dto.getPaisOrigen() != null && dto.getPaisOrigen().getCodigo() != null) {
                    paisRepository.findById(dto.getPaisOrigen().getCodigo())
                        .ifPresent(existingColaboracion::setPaisOrigen);
                }
                if (dto.getCiudadOrigen() != null) {
                    existingColaboracion.setCiudadOrigen(dto.getCiudadOrigen());
                }
                if (dto.getCodigoPaisDestino() != null) {
                    existingColaboracion.setCodigoPaisDestino(dto.getCodigoPaisDestino());
                }
                if (dto.getCiudadDestino() != null) {
                    existingColaboracion.setCiudadDestino(dto.getCiudadDestino());
                }

                // Actualizar participantes
                if (dto.getParticipantes() != null) {
                    participacionProductoRepository.deleteByProductoId(existingColaboracion.getId());
                    if (!dto.getParticipantes().isEmpty()) {
                        saveParticipantes(existingColaboracion, dto.getParticipantes());
                    }
                }

                Colaboracion updated = colaboracionRepository.save(existingColaboracion);
                
                // Recargar la entidad con todas sus relaciones antes de convertir a DTO
                Colaboracion colaboracionConRelaciones = colaboracionRepository.findByIdWithRelations(updated.getId())
                    .orElse(updated);
                
                return ResponseEntity.ok(convertToDTO(colaboracionConRelaciones));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Elimina una colaboración científica
     */
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deleteScientificCollaboration(@PathVariable Long id) {
        return colaboracionRepository.findById(id)
            .map(colaboracion -> {
                // Eliminar archivo PDF asociado si existe
                if (colaboracion.getLinkPDF() != null && !colaboracion.getLinkPDF().trim().isEmpty()) {
                    pdfFileService.deletePdfFile(colaboracion.getLinkPDF());
                }
                // Eliminar participantes asociados
                participacionProductoRepository.deleteByProductoId(id);
                // Eliminar el registro
                colaboracionRepository.deleteById(id);
                return ResponseEntity.noContent().<Void>build();
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Convierte una entidad Colaboracion a DTO (sin participantes - para listas)
     */
    private ColaboracionDTO convertToDTOWithoutParticipants(Colaboracion colaboracion) {
        return convertToDTOWithoutParticipants(colaboracion, null);
    }
    
    /**
     * Convierte una entidad Colaboracion a DTO (sin participantes - para listas) con textos precargados
     */
    private ColaboracionDTO convertToDTOWithoutParticipants(Colaboracion colaboracion, Map<String, String> textosMap) {
        ColaboracionDTO dto = convertToDTOBase(colaboracion, textosMap);
        // No cargar participantes para mejorar rendimiento en listas
        dto.setParticipantes(null);
        return dto;
    }

    /**
     * Convierte una entidad Colaboracion a DTO (con participantes - para detalles)
     */
    private ColaboracionDTO convertToDTO(Colaboracion colaboracion) {
        ColaboracionDTO dto = convertToDTOBase(colaboracion);
        
        // Cargar participantes solo cuando se necesita (detalles)
        List<ParticipacionProducto> participaciones = participacionProductoRepository.findByProductoId(colaboracion.getId());
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
     * Método base para convertir Colaboracion a DTO (sin participantes)
     */
    private ColaboracionDTO convertToDTOBase(Colaboracion colaboracion) {
        return convertToDTOBase(colaboracion, null);
    }
    
    /**
     * Método base para convertir Colaboracion a DTO (sin participantes) con textos precargados
     */
    private ColaboracionDTO convertToDTOBase(Colaboracion colaboracion, Map<String, String> textosMap) {
        ColaboracionDTO dto = new ColaboracionDTO();

        // Campos de ProductoCientifico
        dto.setId(colaboracion.getId());
        if (colaboracion.getDescripcion() != null && !colaboracion.getDescripcion().isEmpty()) {
            String descripcion = textosMap != null && textosMap.containsKey(colaboracion.getDescripcion())
                ? textosMap.get(colaboracion.getDescripcion())
                : textosService.getTextValue(colaboracion.getDescripcion(), 2, "us").orElse(colaboracion.getDescripcion());
            dto.setDescripcion(descripcion);
        }
        if (colaboracion.getComentario() != null && !colaboracion.getComentario().isEmpty()) {
            String comentario = textosMap != null && textosMap.containsKey(colaboracion.getComentario())
                ? textosMap.get(colaboracion.getComentario())
                : textosService.getTextValue(colaboracion.getComentario(), 2, "us").orElse(colaboracion.getComentario());
            dto.setComentario(comentario);
        }
        dto.setFechaInicio(colaboracion.getFechaInicio() != null ? colaboracion.getFechaInicio().toString() : null);
        dto.setFechaTermino(colaboracion.getFechaTermino() != null ? colaboracion.getFechaTermino().toString() : null);
        dto.setUrlDocumento(colaboracion.getUrlDocumento());
        dto.setLinkVisualizacion(colaboracion.getLinkVisualizacion());
        dto.setLinkPDF(colaboracion.getLinkPDF());
        dto.setProgressReport(colaboracion.getProgressReport());
        dto.setCodigoANID(colaboracion.getCodigoANID());
        // Normalizar basal: convertir '1' a 'S' y '0' a 'N', mantener 'S'/'N' como están
        if (colaboracion.getBasal() != null) {
            char basalChar = colaboracion.getBasal();
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
        dto.setLineasInvestigacion(colaboracion.getLineasInvestigacion());
        dto.setCluster(colaboracion.getCluster());
        dto.setParticipantesNombres(colaboracion.getParticipantesNombres());
        dto.setCreatedAt(colaboracion.getCreatedAt() != null ? colaboracion.getCreatedAt().toString() : null);
        dto.setUpdatedAt(colaboracion.getUpdatedAt() != null ? colaboracion.getUpdatedAt().toString() : null);

        // Relaciones de ProductoCientifico
        if (colaboracion.getTipoProducto() != null) {
            TipoProductoDTO tipoDTO = new TipoProductoDTO();
            tipoDTO.setId(colaboracion.getTipoProducto().getId());
            tipoDTO.setIdDescripcion(colaboracion.getTipoProducto().getIdDescripcion());
            tipoDTO.setDescripcion(colaboracion.getTipoProducto().getDescripcion());
            dto.setTipoProducto(tipoDTO);
        }

        if (colaboracion.getEstadoProducto() != null) {
            EstadoProductoDTO estadoDTO = new EstadoProductoDTO();
            estadoDTO.setId(colaboracion.getEstadoProducto().getId());
            estadoDTO.setCodigoDescripcion(colaboracion.getEstadoProducto().getCodigoDescripcion());
            estadoDTO.setCreatedAt(colaboracion.getEstadoProducto().getCreatedAt() != null ?
                colaboracion.getEstadoProducto().getCreatedAt().toString() : null);
            estadoDTO.setUpdatedAt(colaboracion.getEstadoProducto().getUpdatedAt() != null ?
                colaboracion.getEstadoProducto().getUpdatedAt().toString() : null);
            dto.setEstadoProducto(estadoDTO);
        }

        // Campos específicos de Colaboracion
        if (colaboracion.getTipoColaboracion() != null) {
            TipoColaboracionDTO tipoColaboracionDTO = new TipoColaboracionDTO();
            tipoColaboracionDTO.setId(colaboracion.getTipoColaboracion().getId());
            tipoColaboracionDTO.setIdDescripcion(colaboracion.getTipoColaboracion().getIdDescripcion());
            dto.setTipoColaboracion(tipoColaboracionDTO);
        }

        if (colaboracion.getInstitucion() != null) {
            InstitucionDTO institucionDTO = new InstitucionDTO();
            institucionDTO.setId(colaboracion.getInstitucion().getId());
            institucionDTO.setIdDescripcion(colaboracion.getInstitucion().getIdDescripcion());
            institucionDTO.setDescripcion(colaboracion.getInstitucion().getDescripcion());
            dto.setInstitucion(institucionDTO);
        }

        if (colaboracion.getPaisOrigen() != null) {
            PaisDTO paisDTO = new PaisDTO();
            paisDTO.setCodigo(colaboracion.getPaisOrigen().getCodigo());
            paisDTO.setIdDescripcion(colaboracion.getPaisOrigen().getIdDescripcion());
            dto.setPaisOrigen(paisDTO);
        }

        dto.setCiudadOrigen(colaboracion.getCiudadOrigen());
        dto.setCodigoPaisDestino(colaboracion.getCodigoPaisDestino());
        dto.setCiudadDestino(colaboracion.getCiudadDestino());

        return dto;
    }

    /**
     * Convierte un DTO a entidad Colaboracion
     */
    private Colaboracion convertFromDTO(ColaboracionDTO dto) {
        Colaboracion colaboracion = new Colaboracion();

        // Campos de ProductoCientifico
        if (dto.getDescripcion() != null && !dto.getDescripcion().isEmpty()) {
            String codigoDescripcion = textosService.createTextInBothLanguages(dto.getDescripcion(), 2);
            colaboracion.setDescripcion(codigoDescripcion);
        }
        if (dto.getComentario() != null && !dto.getComentario().isEmpty()) {
            String codigoComentario = textosService.createTextInBothLanguages(dto.getComentario(), 2);
            colaboracion.setComentario(codigoComentario);
        }
        // fechaInicio es obligatorio
        if (dto.getFechaInicio() == null || dto.getFechaInicio().isEmpty()) {
            throw new IllegalArgumentException("fechaInicio is required");
        }
        colaboracion.setFechaInicio(parseLocalDate(dto.getFechaInicio()));
        if (dto.getFechaTermino() != null) {
            colaboracion.setFechaTermino(parseLocalDate(dto.getFechaTermino()));
        }
        colaboracion.setUrlDocumento(dto.getUrlDocumento());
        colaboracion.setLinkVisualizacion(dto.getLinkVisualizacion());
        colaboracion.setLinkPDF(dto.getLinkPDF());
        colaboracion.setProgressReport(dto.getProgressReport());
        colaboracion.setCodigoANID(dto.getCodigoANID());
        if (dto.getBasal() != null && !dto.getBasal().isEmpty()) {
            char basalValue = dto.getBasal().charAt(0);
            if (basalValue == 'S' || basalValue == 's' || basalValue == 'N' || basalValue == 'n') {
                colaboracion.setBasal(Character.toUpperCase(basalValue));
            } else {
                colaboracion.setBasal('S');
            }
        } else {
            colaboracion.setBasal('S');
        }
        colaboracion.setLineasInvestigacion(dto.getLineasInvestigacion());
        colaboracion.setCluster(dto.getCluster());

        // Relaciones de ProductoCientifico
        if (dto.getTipoProducto() != null && dto.getTipoProducto().getId() != null) {
            tipoProductoRepository.findById(dto.getTipoProducto().getId())
                .ifPresent(colaboracion::setTipoProducto);
        }

        if (dto.getEstadoProducto() != null && dto.getEstadoProducto().getId() != null) {
            estadoProductoRepository.findById(dto.getEstadoProducto().getId())
                .ifPresent(colaboracion::setEstadoProducto);
        }

        // Campos específicos de Colaboracion
        if (dto.getTipoColaboracion() != null && dto.getTipoColaboracion().getId() != null) {
            tipoColaboracionRepository.findById(dto.getTipoColaboracion().getId())
                .ifPresent(colaboracion::setTipoColaboracion);
        }
        if (dto.getInstitucion() != null && dto.getInstitucion().getId() != null) {
            institucionRepository.findById(dto.getInstitucion().getId())
                .ifPresent(colaboracion::setInstitucion);
        }
        if (dto.getPaisOrigen() != null && dto.getPaisOrigen().getCodigo() != null) {
            paisRepository.findById(dto.getPaisOrigen().getCodigo())
                .ifPresent(colaboracion::setPaisOrigen);
        }
        colaboracion.setCiudadOrigen(dto.getCiudadOrigen());
        colaboracion.setCodigoPaisDestino(dto.getCodigoPaisDestino());
        colaboracion.setCiudadDestino(dto.getCiudadDestino());

        return colaboracion;
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
     * Guarda los participantes de una colaboración científica
     */
    private void saveParticipantes(Colaboracion colaboracion, List<ParticipanteDTO> participantesDTO) {
        for (ParticipanteDTO pDTO : participantesDTO) {
            if (pDTO.getRrhhId() == null || pDTO.getTipoParticipacionId() == null) {
                continue;
            }

            RRHH rrhh = rrhhRepository.findById(pDTO.getRrhhId()).orElse(null);
            TipoParticipacion tipoParticipacion = tipoParticipacionRepository.findById(pDTO.getTipoParticipacionId()).orElse(null);

            if (rrhh != null && tipoParticipacion != null) {
                // Obtener el siguiente ID correlativo para esta combinación (idProducto, idRRHH)
                Long nextId = participacionProductoRepository.getNextIdForParticipacion(
                    colaboracion.getId(), 
                    rrhh.getId()
                );
                
                ParticipacionProducto participacion = new ParticipacionProducto();
                participacion.setRrhh(rrhh);
                participacion.setProducto(colaboracion);
                participacion.setTipoParticipacion(tipoParticipacion);
                participacion.setOrden(pDTO.getOrden() != null ? pDTO.getOrden() : 0);
                participacion.setCorresponding(pDTO.getCorresponding() != null && pDTO.getCorresponding());

                // Establecer el ID compuesto con el correlativo
                ParticipacionProductoId id = new ParticipacionProductoId(
                    rrhh.getId(), 
                    colaboracion.getId(), 
                    nextId
                );
                participacion.setId(id);

                participacionProductoRepository.save(participacion);
            }
        }
    }
}


