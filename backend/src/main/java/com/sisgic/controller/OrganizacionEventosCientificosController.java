package com.sisgic.controller;

import com.sisgic.dto.OrganizacionEventosCientificosDTO;
import com.sisgic.dto.EstadoProductoDTO;
import com.sisgic.dto.ParticipanteDTO;
import com.sisgic.dto.TipoEventoDTO;
import com.sisgic.dto.PaisDTO;
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
@RequestMapping("/api/scientific-events")
@CrossOrigin(origins = "*")
public class OrganizacionEventosCientificosController {

    @Autowired
    private OrganizacionEventosCientificosRepository organizacionEventosCientificosRepository;

    @Autowired
    private TipoEventoRepository tipoEventoRepository;

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
    private PaisRepository paisRepository;

    @Autowired
    private TextosService textosService;

    @Autowired
    private com.sisgic.service.PdfFileService pdfFileService;

    @Autowired
    private com.sisgic.service.UserService userService;

    /**
     * Obtiene todas las organizaciones de eventos científicos con paginación
     */
    @GetMapping
    public ResponseEntity<Page<OrganizacionEventosCientificosDTO>> getScientificEvents(
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
        Page<OrganizacionEventosCientificos> eventos = organizacionEventosCientificosRepository.findVisibleByUserIdRRHH(idRRHH, userName, pageable);

        // Cargar todos los textos de una vez para optimizar consultas
        List<OrganizacionEventosCientificos> eventosList = eventos.getContent();
        List<String> codigosTexto = new ArrayList<>();
        for (OrganizacionEventosCientificos e : eventosList) {
            if (e.getDescripcion() != null && !e.getDescripcion().isEmpty()) {
                codigosTexto.add(e.getDescripcion());
            }
            if (e.getComentario() != null && !e.getComentario().isEmpty()) {
                codigosTexto.add(e.getComentario());
            }
        }
        Map<String, String> textosMap = textosService.getTextValuesBatch(codigosTexto, 2, "us");

        // Para listas, no cargar participantes para mejorar rendimiento
        Page<OrganizacionEventosCientificosDTO> eventosDTO = eventos.map(e -> convertToDTOWithoutParticipants(e, textosMap));

        return ResponseEntity.ok(eventosDTO);
    }

    /**
     * Obtiene una organización de evento científico específica por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrganizacionEventosCientificosDTO> getScientificEvent(@PathVariable Long id) {
        Long idRRHH = userService.getCurrentUserIdRRHH().orElse(null);
        String userName = userService.getCurrentUsername().orElse(null);
        return organizacionEventosCientificosRepository.findVisibleByIdAndUserIdRRHH(id, idRRHH, userName)
            .map(evento -> ResponseEntity.ok(convertToDTO(evento)))
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Crea una nueva organización de evento científico
     */
    @PostMapping
    @Transactional
    public ResponseEntity<OrganizacionEventosCientificosDTO> createScientificEvent(@RequestBody OrganizacionEventosCientificosDTO dto) {
        try {
            OrganizacionEventosCientificos evento = convertFromDTO(dto);
            // Establecer el username del usuario actual solo al crear
            userService.getCurrentUsername().ifPresent(evento::setUsername);
            // Establecer automáticamente el tipoProducto con ID 15 (ORGANIZACION_EVENTOS_CIENTIFICOS)
            TipoProducto tipoProducto = tipoProductoRepository.findById(15L)
                .orElseThrow(() -> new IllegalStateException("TipoProducto with id=15 (ORGANIZACION_EVENTOS_CIENTIFICOS) not found"));
            evento.setTipoProducto(tipoProducto);
            OrganizacionEventosCientificos saved = organizacionEventosCientificosRepository.save(evento);

            if (dto.getParticipantes() != null && !dto.getParticipantes().isEmpty()) {
                saveParticipantes(saved, dto.getParticipantes());
            }

            OrganizacionEventosCientificosDTO resultDTO = convertToDTO(saved);
            return ResponseEntity.ok(resultDTO);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Actualiza una organización de evento científico existente
     */
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<OrganizacionEventosCientificosDTO> updateScientificEvent(
            @PathVariable Long id,
            @RequestBody OrganizacionEventosCientificosDTO dto) {

        return organizacionEventosCientificosRepository.findById(id)
            .map(existingEvento -> {
                // Actualizar campos de ProductoCientifico
                // Actualizar descripción: si ya existe código, actualizar; si no, crear nuevo
                if (dto.getDescripcion() != null && !dto.getDescripcion().isEmpty()) {
                    if (existingEvento.getDescripcion() != null && !existingEvento.getDescripcion().isEmpty()) {
                        // Ya existe código, actualizar el texto
                        textosService.updateTextInBothLanguages(existingEvento.getDescripcion(), dto.getDescripcion(), 2);
                    } else {
                        // No existe código, crear nuevo
                        String codigoDescripcion = textosService.createTextInBothLanguages(dto.getDescripcion(), 2);
                        existingEvento.setDescripcion(codigoDescripcion);
                    }
                }
                // Actualizar comentario: si ya existe código, actualizar; si no, crear nuevo
                if (dto.getComentario() != null && !dto.getComentario().isEmpty()) {
                    if (existingEvento.getComentario() != null && !existingEvento.getComentario().isEmpty()) {
                        // Ya existe código, actualizar el texto
                        textosService.updateTextInBothLanguages(existingEvento.getComentario(), dto.getComentario(), 2);
                    } else {
                        // No existe código, crear nuevo
                        String codigoComentario = textosService.createTextInBothLanguages(dto.getComentario(), 2);
                        existingEvento.setComentario(codigoComentario);
                    }
                }
                // fechaInicio es obligatorio
                if (dto.getFechaInicio() == null || dto.getFechaInicio().isEmpty()) {
                    throw new IllegalArgumentException("fechaInicio is required");
                }
                existingEvento.setFechaInicio(parseLocalDate(dto.getFechaInicio()));
                if (dto.getFechaTermino() != null) {
                    existingEvento.setFechaTermino(parseLocalDate(dto.getFechaTermino()));
                }
                if (dto.getUrlDocumento() != null) {
                    existingEvento.setUrlDocumento(dto.getUrlDocumento());
                }
                if (dto.getLinkVisualizacion() != null) {
                    existingEvento.setLinkVisualizacion(dto.getLinkVisualizacion());
                }
                // Actualizar linkPDF siempre (puede ser null para limpiarlo)
                existingEvento.setLinkPDF(dto.getLinkPDF());
                existingEvento.setProgressReport(dto.getProgressReport());
                if (dto.getCodigoANID() != null) {
                    existingEvento.setCodigoANID(dto.getCodigoANID());
                }
                // Manejar basal: debe ser "S" o "N"
                if (dto.getBasal() != null && !dto.getBasal().isEmpty()) {
                    char basalValue = dto.getBasal().charAt(0);
                    if (basalValue == 'S' || basalValue == 's' || basalValue == 'N' || basalValue == 'n') {
                        existingEvento.setBasal(Character.toUpperCase(basalValue));
                    } else {
                        existingEvento.setBasal('N');
                    }
                } else {
                    existingEvento.setBasal('N');
                }
                if (dto.getLineasInvestigacion() != null) {
                    existingEvento.setLineasInvestigacion(dto.getLineasInvestigacion());
                }
                if (dto.getCluster() != null) {
                    existingEvento.setCluster(dto.getCluster());
                }

                // Actualizar relaciones de ProductoCientifico
                if (dto.getTipoProducto() != null && dto.getTipoProducto().getId() != null) {
                    tipoProductoRepository.findById(dto.getTipoProducto().getId())
                        .ifPresent(existingEvento::setTipoProducto);
                }
                if (dto.getEstadoProducto() != null && dto.getEstadoProducto().getId() != null) {
                    estadoProductoRepository.findById(dto.getEstadoProducto().getId())
                        .ifPresent(existingEvento::setEstadoProducto);
                }

                // Actualizar campos específicos de OrganizacionEventosCientificos
                if (dto.getTipoEvento() != null && dto.getTipoEvento().getId() != null) {
                    tipoEventoRepository.findById(dto.getTipoEvento().getId())
                        .ifPresent(existingEvento::setTipoEvento);
                }
                if (dto.getPais() != null && dto.getPais().getCodigo() != null) {
                    paisRepository.findById(dto.getPais().getCodigo())
                        .ifPresent(existingEvento::setPais);
                }
                if (dto.getCiudad() != null) {
                    existingEvento.setCiudad(dto.getCiudad());
                }
                if (dto.getNumParticipantes() != null) {
                    existingEvento.setNumParticipantes(dto.getNumParticipantes());
                }

                // Actualizar participantes
                if (dto.getParticipantes() != null) {
                    participacionProductoRepository.deleteByProductoId(existingEvento.getId());
                    if (!dto.getParticipantes().isEmpty()) {
                        saveParticipantes(existingEvento, dto.getParticipantes());
                    }
                }

                OrganizacionEventosCientificos updated = organizacionEventosCientificosRepository.save(existingEvento);
                return ResponseEntity.ok(convertToDTO(updated));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Elimina una organización de evento científico
     */
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deleteScientificEvent(@PathVariable Long id) {
        return organizacionEventosCientificosRepository.findById(id)
            .map(evento -> {
                // Eliminar archivo PDF asociado si existe
                if (evento.getLinkPDF() != null && !evento.getLinkPDF().trim().isEmpty()) {
                    pdfFileService.deletePdfFile(evento.getLinkPDF());
                }
                // Eliminar participantes asociados
                participacionProductoRepository.deleteByProductoId(id);
                // Eliminar el registro
                organizacionEventosCientificosRepository.deleteById(id);
                return ResponseEntity.noContent().<Void>build();
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Convierte una entidad OrganizacionEventosCientificos a DTO (sin participantes - para listas)
     */
    private OrganizacionEventosCientificosDTO convertToDTOWithoutParticipants(OrganizacionEventosCientificos evento) {
        return convertToDTOWithoutParticipants(evento, null);
    }
    
    /**
     * Convierte una entidad OrganizacionEventosCientificos a DTO (sin participantes - para listas) con textos precargados
     */
    private OrganizacionEventosCientificosDTO convertToDTOWithoutParticipants(OrganizacionEventosCientificos evento, Map<String, String> textosMap) {
        OrganizacionEventosCientificosDTO dto = convertToDTOBase(evento, textosMap);
        // No cargar participantes para mejorar rendimiento en listas
        dto.setParticipantes(null);
        return dto;
    }

    /**
     * Convierte una entidad OrganizacionEventosCientificos a DTO (con participantes - para detalles)
     */
    private OrganizacionEventosCientificosDTO convertToDTO(OrganizacionEventosCientificos evento) {
        OrganizacionEventosCientificosDTO dto = convertToDTOBase(evento);
        
        // Cargar participantes solo cuando se necesita (detalles)
        List<ParticipacionProducto> participaciones = participacionProductoRepository.findByProductoId(evento.getId());
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
     * Método base para convertir OrganizacionEventosCientificos a DTO (sin participantes)
     */
    private OrganizacionEventosCientificosDTO convertToDTOBase(OrganizacionEventosCientificos evento) {
        return convertToDTOBase(evento, null);
    }
    
    /**
     * Método base para convertir OrganizacionEventosCientificos a DTO (sin participantes) con textos precargados
     */
    private OrganizacionEventosCientificosDTO convertToDTOBase(OrganizacionEventosCientificos evento, Map<String, String> textosMap) {
        OrganizacionEventosCientificosDTO dto = new OrganizacionEventosCientificosDTO();

        // Campos de ProductoCientifico
        dto.setId(evento.getId());
        // Obtener el valor del texto desde la tabla textos usando el código guardado en idDescripcion
        if (evento.getDescripcion() != null && !evento.getDescripcion().isEmpty()) {
            String descripcion = textosMap != null && textosMap.containsKey(evento.getDescripcion())
                ? textosMap.get(evento.getDescripcion())
                : textosService.getTextValue(evento.getDescripcion(), 2, "us").orElse(evento.getDescripcion());
            dto.setDescripcion(descripcion);
        }
        // Obtener el valor del texto desde la tabla textos usando el código guardado en idComentario
        if (evento.getComentario() != null && !evento.getComentario().isEmpty()) {
            String comentario = textosMap != null && textosMap.containsKey(evento.getComentario())
                ? textosMap.get(evento.getComentario())
                : textosService.getTextValue(evento.getComentario(), 2, "us").orElse(evento.getComentario());
            dto.setComentario(comentario);
        }
        dto.setFechaInicio(evento.getFechaInicio() != null ? evento.getFechaInicio().toString() : null);
        dto.setFechaTermino(evento.getFechaTermino() != null ? evento.getFechaTermino().toString() : null);
        dto.setUrlDocumento(evento.getUrlDocumento());
        dto.setLinkVisualizacion(evento.getLinkVisualizacion());
        dto.setLinkPDF(evento.getLinkPDF());
        dto.setProgressReport(evento.getProgressReport());
        dto.setCodigoANID(evento.getCodigoANID());
        // Normalizar basal: convertir '1' a 'S' y '0' a 'N', mantener 'S'/'N' como están
        if (evento.getBasal() != null) {
            char basalChar = evento.getBasal();
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
        dto.setLineasInvestigacion(evento.getLineasInvestigacion());
        dto.setCluster(evento.getCluster());
        dto.setParticipantesNombres(evento.getParticipantesNombres());
        dto.setCreatedAt(evento.getCreatedAt() != null ? evento.getCreatedAt().toString() : null);
        dto.setUpdatedAt(evento.getUpdatedAt() != null ? evento.getUpdatedAt().toString() : null);
        dto.setOrganizer(evento.getOrganizer());

        // Relaciones de ProductoCientifico
        if (evento.getTipoProducto() != null) {
            TipoProductoDTO tipoDTO = new TipoProductoDTO();
            tipoDTO.setId(evento.getTipoProducto().getId());
            tipoDTO.setIdDescripcion(evento.getTipoProducto().getIdDescripcion());
            tipoDTO.setDescripcion(evento.getTipoProducto().getDescripcion());
            dto.setTipoProducto(tipoDTO);
        }

        if (evento.getEstadoProducto() != null) {
            EstadoProductoDTO estadoDTO = new EstadoProductoDTO();
            estadoDTO.setId(evento.getEstadoProducto().getId());
            estadoDTO.setCodigoDescripcion(evento.getEstadoProducto().getCodigoDescripcion());
            estadoDTO.setCreatedAt(evento.getEstadoProducto().getCreatedAt() != null ?
                evento.getEstadoProducto().getCreatedAt().toString() : null);
            estadoDTO.setUpdatedAt(evento.getEstadoProducto().getUpdatedAt() != null ?
                evento.getEstadoProducto().getUpdatedAt().toString() : null);
            dto.setEstadoProducto(estadoDTO);
        }

        // Campos específicos de OrganizacionEventosCientificos
        if (evento.getTipoEvento() != null) {
            TipoEventoDTO tipoEventoDTO = new TipoEventoDTO();
            tipoEventoDTO.setId(evento.getTipoEvento().getId());
            tipoEventoDTO.setIdDescripcion(evento.getTipoEvento().getIdDescripcion());
            tipoEventoDTO.setDescripcion(evento.getTipoEvento().getDescripcion());
            tipoEventoDTO.setCreatedAt(evento.getTipoEvento().getCreatedAt() != null ?
                evento.getTipoEvento().getCreatedAt().toString() : null);
            tipoEventoDTO.setUpdatedAt(evento.getTipoEvento().getUpdatedAt() != null ?
                evento.getTipoEvento().getUpdatedAt().toString() : null);
            dto.setTipoEvento(tipoEventoDTO);
        }

        if (evento.getPais() != null) {
            PaisDTO paisDTO = new PaisDTO();
            paisDTO.setCodigo(evento.getPais().getCodigo());
            paisDTO.setIdDescripcion(evento.getPais().getIdDescripcion());
            dto.setPais(paisDTO);
        }

        dto.setCiudad(evento.getCiudad());
        dto.setNumParticipantes(evento.getNumParticipantes());

        return dto;
    }

    /**
     * Convierte un DTO a entidad OrganizacionEventosCientificos
     */
    private OrganizacionEventosCientificos convertFromDTO(OrganizacionEventosCientificosDTO dto) {
        OrganizacionEventosCientificos evento = new OrganizacionEventosCientificos();

        // Campos de ProductoCientifico
        // Generar código de texto para descripción si se proporciona
        if (dto.getDescripcion() != null && !dto.getDescripcion().isEmpty()) {
            String codigoDescripcion = textosService.createTextInBothLanguages(dto.getDescripcion(), 2);
            evento.setDescripcion(codigoDescripcion); // Guardar el código en idDescripcion
        }
        // Generar código de texto para comentario si se proporciona
        if (dto.getComentario() != null && !dto.getComentario().isEmpty()) {
            String codigoComentario = textosService.createTextInBothLanguages(dto.getComentario(), 2);
            evento.setComentario(codigoComentario); // Guardar el código en idComentario
        }
        // fechaInicio es obligatorio
        if (dto.getFechaInicio() == null || dto.getFechaInicio().isEmpty()) {
            throw new IllegalArgumentException("fechaInicio is required");
        }
        evento.setFechaInicio(parseLocalDate(dto.getFechaInicio()));
        if (dto.getFechaTermino() != null) {
            evento.setFechaTermino(parseLocalDate(dto.getFechaTermino()));
        }
        evento.setUrlDocumento(dto.getUrlDocumento());
        evento.setLinkVisualizacion(dto.getLinkVisualizacion());
        evento.setLinkPDF(dto.getLinkPDF());
        evento.setProgressReport(dto.getProgressReport());
        evento.setCodigoANID(dto.getCodigoANID());
        // Manejar basal: debe ser "S" o "N"
        if (dto.getBasal() != null && !dto.getBasal().isEmpty()) {
            char basalValue = dto.getBasal().charAt(0);
            if (basalValue == 'S' || basalValue == 's' || basalValue == 'N' || basalValue == 'n') {
                evento.setBasal(Character.toUpperCase(basalValue));
            } else {
                evento.setBasal('S');
            }
        } else {
            evento.setBasal('S');
        }
        evento.setLineasInvestigacion(dto.getLineasInvestigacion());
        evento.setCluster(dto.getCluster());

        // Relaciones de ProductoCientifico
        if (dto.getTipoProducto() != null && dto.getTipoProducto().getId() != null) {
            tipoProductoRepository.findById(dto.getTipoProducto().getId())
                .ifPresent(evento::setTipoProducto);
        }

        if (dto.getEstadoProducto() != null && dto.getEstadoProducto().getId() != null) {
            estadoProductoRepository.findById(dto.getEstadoProducto().getId())
                .ifPresent(evento::setEstadoProducto);
        }

        // Campos específicos de OrganizacionEventosCientificos
        if (dto.getTipoEvento() != null && dto.getTipoEvento().getId() != null) {
            tipoEventoRepository.findById(dto.getTipoEvento().getId())
                .ifPresent(evento::setTipoEvento);
        }
        if (dto.getPais() != null && dto.getPais().getCodigo() != null) {
            paisRepository.findById(dto.getPais().getCodigo())
                .ifPresent(evento::setPais);
        }
        evento.setCiudad(dto.getCiudad());
        evento.setNumParticipantes(dto.getNumParticipantes());

        return evento;
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
     * Guarda los participantes de una organización de evento científico
     */
    private void saveParticipantes(OrganizacionEventosCientificos evento, List<ParticipanteDTO> participantesDTO) {
        for (ParticipanteDTO pDTO : participantesDTO) {
            if (pDTO.getRrhhId() == null || pDTO.getTipoParticipacionId() == null) {
                continue; // Saltar si faltan datos requeridos
            }

            RRHH rrhh = rrhhRepository.findById(pDTO.getRrhhId()).orElse(null);
            TipoParticipacion tipoParticipacion = tipoParticipacionRepository.findById(pDTO.getTipoParticipacionId()).orElse(null);

            if (rrhh != null && tipoParticipacion != null) {
                // Obtener el siguiente ID correlativo para esta combinación (idProducto, idRRHH)
                Long nextId = participacionProductoRepository.getNextIdForParticipacion(
                    evento.getId(), 
                    rrhh.getId()
                );
                
                ParticipacionProducto participacion = new ParticipacionProducto();
                participacion.setRrhh(rrhh);
                participacion.setProducto(evento);
                participacion.setTipoParticipacion(tipoParticipacion);
                participacion.setOrden(pDTO.getOrden() != null ? pDTO.getOrden() : 0);
                participacion.setCorresponding(pDTO.getCorresponding() != null && pDTO.getCorresponding());

                // Establecer el ID compuesto con el correlativo
                ParticipacionProductoId id = new ParticipacionProductoId(
                    rrhh.getId(), 
                    evento.getId(), 
                    nextId
                );
                participacion.setId(id);

                participacionProductoRepository.save(participacion);
            }
        }
    }
}

