package com.sisgic.controller;

import com.sisgic.dto.PublicacionDTO;
import com.sisgic.dto.JournalDTO;
import com.sisgic.dto.TipoProductoDTO;
import com.sisgic.dto.EstadoProductoDTO;
import com.sisgic.dto.ParticipanteDTO;
import com.sisgic.dto.AfiliacionDTO;
import com.sisgic.dto.PublicationPreviewDTO;
import com.sisgic.dto.PublicationImportRequestDTO;
import com.sisgic.dto.AuthorImportDecisionDTO;
import com.sisgic.dto.AffiliationImportDecisionDTO;
import com.sisgic.dto.JournalImportDecisionDTO;
import com.sisgic.dto.PublicationPreviewDataDTO;
import com.sisgic.entity.*;
import com.sisgic.repository.*;
import com.sisgic.service.TextosService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/publications")
@CrossOrigin(origins = "*")
public class PublicacionController {

    @Autowired
    private PublicacionRepository publicacionRepository;
    
    @Autowired
    private JournalRepository journalRepository;
    
    @Autowired
    private TipoProductoRepository tipoProductoRepository;
    
    @Autowired
    private EstadoProductoRepository estadoProductoRepository;
    
    @Autowired
    private RRHHRepository rrhhRepository;
    
    @Autowired
    private ProyectoRepository proyectoRepository;
    
    @Autowired
    private TipoParticipacionRepository tipoParticipacionRepository;
    
    @Autowired
    private ParticipacionProductoRepository participacionProductoRepository;

    @Autowired
    private TextosService textosService;

    @Autowired
    private com.sisgic.service.PdfFileService pdfFileService;
    
    @Autowired
    private AfiliacionRepository afiliacionRepository;
    
    @Autowired
    private InstitucionRepository institucionRepository;
    
    @Autowired
    private com.sisgic.service.OpenAlexService openAlexService;
    
    @Autowired
    private com.sisgic.service.ResearcherMatchingService researcherMatchingService;
    
    @Autowired
    private ProductoProyectoRepository productoProyectoRepository;
    
    @Autowired
    private com.sisgic.service.UserService userService;
    
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PublicacionController.class);

    /**
     * Obtiene todas las publicaciones con paginación
     */
    @GetMapping
    public ResponseEntity<Page<PublicacionDTO>> getPublications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : 
            Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        
        // Obtener idRRHH y userName del usuario conectado
        java.util.Optional<Long> idRRHHOpt = userService.getCurrentUserIdRRHH();
        Long idRRHH = idRRHHOpt.orElse(null);
        String userName = userService.getCurrentUsername().orElse(null);
        // idRRHH y userName pueden ser null, en ese caso la función retorna 1 (muestra todos)
        Page<Publicacion> publicaciones = publicacionRepository.findVisibleByUserIdRRHH(idRRHH, userName, pageable);
        
        // Cargar todos los textos de una vez para optimizar consultas
        List<Publicacion> publicacionesList = publicaciones.getContent();
        List<String> codigosTexto = new ArrayList<>();
        for (Publicacion p : publicacionesList) {
            if (p.getDescripcion() != null && !p.getDescripcion().isEmpty()) {
                codigosTexto.add(p.getDescripcion());
            }
            if (p.getComentario() != null && !p.getComentario().isEmpty()) {
                codigosTexto.add(p.getComentario());
            }
        }
        Map<String, String> textosMap = textosService.getTextValuesBatch(codigosTexto, 2, "us");
        
        // Para listas, no cargar participantes para mejorar rendimiento
        Page<PublicacionDTO> publicacionesDTO = publicaciones.map(p -> convertToDTOWithoutParticipants(p, textosMap));
        
        return ResponseEntity.ok(publicacionesDTO);
    }

    /**
     * Obtiene una publicación específica por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<PublicacionDTO> getPublication(@PathVariable Long id) {
        Long idRRHH = userService.getCurrentUserIdRRHH().orElse(null);
        String userName = userService.getCurrentUsername().orElse(null);
        return publicacionRepository.findVisibleByIdAndUserIdRRHH(id, idRRHH, userName)
            .map(publicacion -> ResponseEntity.ok(convertToDTO(publicacion)))
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Crea una nueva publicación
     * Primero se crea en la tabla producto, luego en publicacion
     */
    @PostMapping
    @Transactional
    public ResponseEntity<PublicacionDTO> createPublication(@RequestBody PublicacionDTO dto) {
        try {
            // Crear la entidad Publicacion (que extiende ProductoCientifico)
            // JPA con JOINED inheritance automáticamente crea primero en producto y luego en publicacion
            Publicacion publicacion = convertFromDTO(dto);
            
            // Establecer el username del usuario actual solo al crear
            userService.getCurrentUsername().ifPresent(publicacion::setUsername);
            
            // Guardar la publicación (esto crea primero en producto y luego en publicacion)
            Publicacion saved = publicacionRepository.save(publicacion);
            
            // Guardar participantes si están presentes en el DTO
            if (dto.getParticipantes() != null && !dto.getParticipantes().isEmpty()) {
                saveParticipantes(saved, dto.getParticipantes());
            }
            
            // Verificar si hay autores sin afiliaciones y establecer el estado
            boolean hasAuthorsWithoutAffiliations = hasAuthorsWithoutAffiliations(saved);
            EstadoProducto estadoProducto;
            if (hasAuthorsWithoutAffiliations) {
                // Si hay autores sin afiliaciones → Publication pending (id = 4)
                estadoProducto = estadoProductoRepository.findById(4L)
                    .orElseThrow(() -> new IllegalStateException("EstadoProducto with id=4 (Publication pending) not found"));
            } else {
                // Si todos los autores tienen afiliaciones → Publication complete (id = 3)
                estadoProducto = estadoProductoRepository.findById(3L)
                    .orElseThrow(() -> new IllegalStateException("EstadoProducto with id=3 (Publication complete) not found"));
            }
            saved.setEstadoProducto(estadoProducto);
            saved = publicacionRepository.save(saved);
            
            PublicacionDTO resultDTO = convertToDTO(saved);
            return ResponseEntity.ok(resultDTO);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Actualiza una publicación existente
     */
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<PublicacionDTO> updatePublication(
            @PathVariable Long id, 
            @RequestBody PublicacionDTO dto) {
        
        return publicacionRepository.findById(id)
            .map(existingPublication -> {
                // Actualizar campos de ProductoCientifico
                // Actualizar descripción: si ya existe código, actualizar; si no, crear nuevo
                if (dto.getDescripcion() != null && !dto.getDescripcion().isEmpty()) {
                    if (existingPublication.getDescripcion() != null && !existingPublication.getDescripcion().isEmpty()) {
                        // Ya existe código, actualizar el texto
                        textosService.updateTextInBothLanguages(existingPublication.getDescripcion(), dto.getDescripcion(), 2);
                    } else {
                        // No existe código, crear nuevo
                        String codigoDescripcion = textosService.createTextInBothLanguages(dto.getDescripcion(), 2);
                        existingPublication.setDescripcion(codigoDescripcion);
                    }
                }
                // Actualizar comentario: si ya existe código, actualizar; si no, crear nuevo
                if (dto.getComentario() != null && !dto.getComentario().isEmpty()) {
                    if (existingPublication.getComentario() != null && !existingPublication.getComentario().isEmpty()) {
                        // Ya existe código, actualizar el texto
                        textosService.updateTextInBothLanguages(existingPublication.getComentario(), dto.getComentario(), 2);
                    } else {
                        // No existe código, crear nuevo
                        String codigoComentario = textosService.createTextInBothLanguages(dto.getComentario(), 2);
                        existingPublication.setComentario(codigoComentario);
                    }
                }
                // fechaInicio es obligatorio
                if (dto.getFechaInicio() == null || dto.getFechaInicio().isEmpty()) {
                    throw new IllegalArgumentException("fechaInicio is required");
                }
                existingPublication.setFechaInicio(parseLocalDate(dto.getFechaInicio()));
                if (dto.getFechaTermino() != null) {
                    existingPublication.setFechaTermino(parseLocalDate(dto.getFechaTermino()));
                }
                if (dto.getUrlDocumento() != null) {
                    existingPublication.setUrlDocumento(dto.getUrlDocumento());
                }
                if (dto.getLinkVisualizacion() != null) {
                    existingPublication.setLinkVisualizacion(dto.getLinkVisualizacion());
                }
                // Actualizar linkPDF siempre (puede ser null para limpiarlo)
                existingPublication.setLinkPDF(dto.getLinkPDF());
                if (dto.getProgressReport() != null) {
                    existingPublication.setProgressReport(dto.getProgressReport());
                }
                if (dto.getCodigoANID() != null) {
                    existingPublication.setCodigoANID(dto.getCodigoANID());
                }
                // Manejar basal: debe ser "S" o "N"
                if (dto.getBasal() != null && !dto.getBasal().isEmpty()) {
                    char basalValue = dto.getBasal().charAt(0);
                    // Validar que sea S o N
                    if (basalValue == 'S' || basalValue == 's' || basalValue == 'N' || basalValue == 'n') {
                        existingPublication.setBasal(Character.toUpperCase(basalValue));
                    } else {
                        // Si no es válido, establecer por defecto "N"
                        existingPublication.setBasal('N');
                    }
                } else {
                    // Si viene null o vacío, establecer por defecto "N"
                    existingPublication.setBasal('N');
                }
                if (dto.getLineasInvestigacion() != null) {
                    existingPublication.setLineasInvestigacion(dto.getLineasInvestigacion());
                }
                if (dto.getCluster() != null) {
                    existingPublication.setCluster(dto.getCluster());
                }
                
                // Actualizar relaciones de ProductoCientifico
                if (dto.getTipoProducto() != null && dto.getTipoProducto().getId() != null) {
                    tipoProductoRepository.findById(dto.getTipoProducto().getId())
                        .ifPresent(existingPublication::setTipoProducto);
                }
                if (dto.getEstadoProducto() != null && dto.getEstadoProducto().getId() != null) {
                    estadoProductoRepository.findById(dto.getEstadoProducto().getId())
                        .ifPresent(existingPublication::setEstadoProducto);
                }
                
                // Actualizar campos específicos de Publicacion
                if (dto.getJournal() != null && dto.getJournal().getId() != null) {
                    journalRepository.findById(dto.getJournal().getId())
                        .ifPresent(existingPublication::setJournal);
                }
                if (dto.getVolume() != null) {
                    existingPublication.setVolume(dto.getVolume());
                }
                if (dto.getYearPublished() != null) {
                    existingPublication.setYearPublished(dto.getYearPublished());
                }
                if (dto.getFirstpage() != null) {
                    existingPublication.setFirstpage(dto.getFirstpage());
                }
                if (dto.getLastpage() != null) {
                    existingPublication.setLastpage(dto.getLastpage());
                }
                if (dto.getIndexs() != null) {
                    existingPublication.setIndexs(dto.getIndexs());
                }
                if (dto.getFunding() != null) {
                    existingPublication.setFunding(dto.getFunding());
                }
                if (dto.getDoi() != null) {
                    existingPublication.setDoi(dto.getDoi());
                }
                if (dto.getNumCitas() != null) {
                    existingPublication.setNumCitas(dto.getNumCitas());
                }
                
                // Actualizar participantes: borrar todos y recrear desde el DTO
                // (las afiliaciones se recrean usando los datos que vienen desde el frontend)
                if (dto.getParticipantes() != null) {
                    participacionProductoRepository.deleteByProductoId(existingPublication.getId());
                    if (!dto.getParticipantes().isEmpty()) {
                        saveParticipantes(existingPublication, dto.getParticipantes());
                    }
                }
                
                Publicacion updated = publicacionRepository.save(existingPublication);
                return ResponseEntity.ok(convertToDTO(updated));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Elimina una publicación
     */
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deletePublication(@PathVariable Long id) {
        return publicacionRepository.findById(id)
            .map(publicacion -> {
                // Eliminar archivo PDF asociado si existe
                if (publicacion.getLinkPDF() != null && !publicacion.getLinkPDF().trim().isEmpty()) {
                    pdfFileService.deletePdfFile(publicacion.getLinkPDF());
                }
                // Eliminar el registro
                publicacionRepository.deleteById(id);
                return ResponseEntity.noContent().<Void>build();
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Busca publicaciones por DOI
     */
    @GetMapping("/search/doi/{doi}")
    public ResponseEntity<PublicacionDTO> searchByDOI(@PathVariable String doi) {
        return publicacionRepository.findByDoi(doi)
            .map(publicacion -> ResponseEntity.ok(convertToDTO(publicacion)))
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Obtiene publicaciones por año
     */
    @GetMapping("/year/{year}")
    public ResponseEntity<List<PublicacionDTO>> getPublicationsByYear(@PathVariable Integer year) {
        List<Publicacion> publicaciones = publicacionRepository.findByYearPublished(year);
        List<PublicacionDTO> publicacionesDTO = publicaciones.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(publicacionesDTO);
    }

    /**
     * Obtiene estadísticas de publicaciones
     */
    @GetMapping("/stats")
    public ResponseEntity<Object> getPublicationStats() {
        long totalPublications = publicacionRepository.count();
        long publicationsWithDOI = publicacionRepository.findAll().stream()
            .filter(p -> p.getDoi() != null && !p.getDoi().isEmpty())
            .count();
        
        return ResponseEntity.ok(new PublicationStats(totalPublications, publicationsWithDOI));
    }

    /**
     * Obtiene los factores de impacto de un journal para un año específico
     * Usa las funciones de MySQL f_getImpactFactor y f_getAvgImpactFactor
     */
    @GetMapping("/impact-factors")
    public ResponseEntity<ImpactFactorsDTO> getImpactFactors(
            @RequestParam Long journalId,
            @RequestParam Integer year) {
        
        try {
            // Llamar a las funciones de MySQL para obtener los factores de impacto
            PublicacionRepository.ImpactFactorsResult result = 
                publicacionRepository.getImpactFactors(journalId, year);
            
            ImpactFactorsDTO factors = new ImpactFactorsDTO();
            
            if (result != null && result.getImpactFactor() != null) {
                factors.setFactorImpacto(result.getImpactFactor().doubleValue());
            } else {
                factors.setFactorImpacto(null);
            }
            
            if (result != null && result.getAvgImpactFactor() != null) {
                factors.setFactorImpactoPromedio(result.getAvgImpactFactor().doubleValue());
            } else {
                factors.setFactorImpactoPromedio(null);
            }
            
            return ResponseEntity.ok(factors);
        } catch (Exception e) {
            // En caso de error, retornar null para ambos factores
            System.err.println("Error obteniendo factores de impacto: " + e.getMessage());
            e.printStackTrace();
            
            ImpactFactorsDTO factors = new ImpactFactorsDTO();
            factors.setFactorImpacto(null);
            factors.setFactorImpactoPromedio(null);
            return ResponseEntity.ok(factors);
        }
    }

    // Clase interna para factores de impacto
    public static class ImpactFactorsDTO {
        private Double factorImpacto;
        private Double factorImpactoPromedio;
        
        public Double getFactorImpacto() {
            return factorImpacto;
        }
        
        public void setFactorImpacto(Double factorImpacto) {
            this.factorImpacto = factorImpacto;
        }
        
        public Double getFactorImpactoPromedio() {
            return factorImpactoPromedio;
        }
        
        public void setFactorImpactoPromedio(Double factorImpactoPromedio) {
            this.factorImpactoPromedio = factorImpactoPromedio;
        }
    }

    // Clase interna para las estadísticas
    public static class PublicationStats {
        public final long totalPublications;
        public final long publicationsWithDOI;
        
        public PublicationStats(long totalPublications, long publicationsWithDOI) {
            this.totalPublications = totalPublications;
            this.publicationsWithDOI = publicationsWithDOI;
        }
    }

    /**
     * Convierte una entidad Publicacion a DTO (sin participantes - para listas)
     */
    private PublicacionDTO convertToDTOWithoutParticipants(Publicacion publicacion) {
        return convertToDTOWithoutParticipants(publicacion, null);
    }
    
    /**
     * Convierte una entidad Publicacion a DTO (sin participantes - para listas) con textos precargados
     */
    private PublicacionDTO convertToDTOWithoutParticipants(Publicacion publicacion, Map<String, String> textosMap) {
        PublicacionDTO dto = convertToDTOBase(publicacion, textosMap);
        // No cargar participantes para mejorar rendimiento en listas
        dto.setParticipantes(null);
        return dto;
    }

    /**
     * Convierte una entidad Publicacion a DTO (con participantes - para detalles)
     */
    private PublicacionDTO convertToDTO(Publicacion publicacion) {
        PublicacionDTO dto = convertToDTOBase(publicacion);
        
        // Cargar participantes solo cuando se necesita (detalles)
        List<ParticipacionProducto> participaciones = participacionProductoRepository.findByProductoId(publicacion.getId());
        List<ParticipanteDTO> participantesDTO = new ArrayList<>();
        
        for (ParticipacionProducto pp : participaciones) {
            ParticipanteDTO pDTO = new ParticipanteDTO();
            pDTO.setRrhhId(pp.getRrhh() != null ? pp.getRrhh().getId() : null);
            pDTO.setTipoParticipacionId(pp.getTipoParticipacion() != null ? pp.getTipoParticipacion().getId() : null);
            pDTO.setOrden(pp.getOrden());
            pDTO.setCorresponding(pp.isCorresponding());
            
            Long rrhhId = pp.getRrhh() != null ? pp.getRrhh().getId() : null;
            Long productoId = publicacion.getId();
            Long rrhhProductoId = (pp.getId() != null ? pp.getId().getId() : null);
            
            if (rrhhProductoId != null) {
                pDTO.setIdRRHHProducto(rrhhProductoId);
                
                // Cargar afiliaciones asociadas a esta participación
                List<Afiliacion> afiliaciones = afiliacionRepository.findByParticipacion(rrhhId, productoId, rrhhProductoId);
                List<AfiliacionDTO> afiliacionesDTO = afiliaciones.stream()
                    .map(this::convertToAfiliacionDTO)
                    .collect(Collectors.toList());
                pDTO.setAfiliaciones(afiliacionesDTO);
            }
            
            participantesDTO.add(pDTO);
        }
        
        dto.setParticipantes(participantesDTO);
        
        return dto;
    }

    /**
     * Método base para convertir Publicacion a DTO (sin participantes)
     */
    private PublicacionDTO convertToDTOBase(Publicacion publicacion) {
        return convertToDTOBase(publicacion, null);
    }
    
    /**
     * Método base para convertir Publicacion a DTO (sin participantes) con textos precargados
     */
    private PublicacionDTO convertToDTOBase(Publicacion publicacion, Map<String, String> textosMap) {
        PublicacionDTO dto = new PublicacionDTO();
        
        // Campos de ProductoCientifico
        dto.setId(publicacion.getId());
        // Obtener el valor del texto desde la tabla textos usando el código guardado en idDescripcion
        if (publicacion.getDescripcion() != null && !publicacion.getDescripcion().isEmpty()) {
            String descripcion = textosMap != null && textosMap.containsKey(publicacion.getDescripcion())
                ? textosMap.get(publicacion.getDescripcion())
                : textosService.getTextValue(publicacion.getDescripcion(), 2, "us").orElse(publicacion.getDescripcion());
            dto.setDescripcion(descripcion);
        }
        // Obtener el valor del texto desde la tabla textos usando el código guardado en idComentario
        if (publicacion.getComentario() != null && !publicacion.getComentario().isEmpty()) {
            String comentario = textosMap != null && textosMap.containsKey(publicacion.getComentario())
                ? textosMap.get(publicacion.getComentario())
                : textosService.getTextValue(publicacion.getComentario(), 2, "us").orElse(publicacion.getComentario());
            dto.setComentario(comentario);
        }
        dto.setFechaInicio(publicacion.getFechaInicio() != null ? publicacion.getFechaInicio().toString() : null);
        dto.setFechaTermino(publicacion.getFechaTermino() != null ? publicacion.getFechaTermino().toString() : null);
        dto.setUrlDocumento(publicacion.getUrlDocumento());
        dto.setLinkVisualizacion(publicacion.getLinkVisualizacion());
        dto.setLinkPDF(publicacion.getLinkPDF());
        dto.setProgressReport(publicacion.getProgressReport());
        dto.setCodigoANID(publicacion.getCodigoANID());
        // Normalizar basal: convertir '1' a 'S' y '0' a 'N', mantener 'S'/'N' como están
        if (publicacion.getBasal() != null) {
            char basalChar = publicacion.getBasal();
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
        dto.setLineasInvestigacion(publicacion.getLineasInvestigacion());
        dto.setCluster(publicacion.getCluster());
        dto.setParticipantesNombres(publicacion.getParticipantesNombres());
        dto.setCreatedAt(publicacion.getCreatedAt() != null ? publicacion.getCreatedAt().toString() : null);
        dto.setUpdatedAt(publicacion.getUpdatedAt() != null ? publicacion.getUpdatedAt().toString() : null);
        
        // Relaciones de ProductoCientifico
        if (publicacion.getTipoProducto() != null) {
            TipoProductoDTO tipoDTO = new TipoProductoDTO();
            tipoDTO.setId(publicacion.getTipoProducto().getId());
            tipoDTO.setIdDescripcion(publicacion.getTipoProducto().getIdDescripcion());
            tipoDTO.setDescripcion(publicacion.getTipoProducto().getDescripcion());
            dto.setTipoProducto(tipoDTO);
        }
        
        if (publicacion.getEstadoProducto() != null) {
            EstadoProductoDTO estadoDTO = new EstadoProductoDTO();
            estadoDTO.setId(publicacion.getEstadoProducto().getId());
            estadoDTO.setCodigoDescripcion(publicacion.getEstadoProducto().getCodigoDescripcion());
            estadoDTO.setCreatedAt(publicacion.getEstadoProducto().getCreatedAt() != null ? 
                publicacion.getEstadoProducto().getCreatedAt().toString() : null);
            estadoDTO.setUpdatedAt(publicacion.getEstadoProducto().getUpdatedAt() != null ? 
                publicacion.getEstadoProducto().getUpdatedAt().toString() : null);
            dto.setEstadoProducto(estadoDTO);
        }
        
        // Campos específicos de Publicacion
        if (publicacion.getJournal() != null) {
            // Obtener journal con descripción resuelta desde textos
            JournalDTO journalDTO = journalRepository.findByIdWithDescription(
                publicacion.getJournal().getId(), 
                "us"
            ).orElseGet(() -> {
                // Fallback si no se encuentra con descripción
                JournalDTO fallback = new JournalDTO();
                fallback.setId(publicacion.getJournal().getId());
                fallback.setIdDescripcion(publicacion.getJournal().getIdDescripcion());
                fallback.setAbbreviation(publicacion.getJournal().getAbbreviation());
                fallback.setIssn(publicacion.getJournal().getIssn());
                fallback.setCreatedAt(publicacion.getJournal().getCreatedAt());
                fallback.setUpdatedAt(publicacion.getJournal().getUpdatedAt());
                return fallback;
            });
            dto.setJournal(journalDTO);
            
            // Factor de impacto del Journal (si está disponible)
            // Por ahora lo dejamos null ya que viene de la relación con Journal
            dto.setFactorImpacto(null);
            dto.setFactorImpactoPromedio(null);
        }
        
        // Factores de impacto calculados por el proceso batch (almacenados en la publicación)
        if (publicacion.getImpactFactor() != null) {
            dto.setFactorImpacto(publicacion.getImpactFactor().doubleValue());
        } else {
            dto.setFactorImpacto(null);
        }
        
        if (publicacion.getAvgImpactFactor() != null) {
            dto.setFactorImpactoPromedio(publicacion.getAvgImpactFactor().doubleValue());
        } else {
            dto.setFactorImpactoPromedio(null);
        }
        
        dto.setVolume(publicacion.getVolume());
        dto.setYearPublished(publicacion.getYearPublished());
        dto.setFirstpage(publicacion.getFirstpage());
        dto.setLastpage(publicacion.getLastpage());
        dto.setIndexs(publicacion.getIndexs());
        dto.setFunding(publicacion.getFunding());
        dto.setDoi(publicacion.getDoi());
        dto.setNumCitas(publicacion.getNumCitas());
        
        return dto;
    }

    /**
     * Convierte un DTO a entidad Publicacion
     */
    private Publicacion convertFromDTO(PublicacionDTO dto) {
        Publicacion publicacion = new Publicacion();
        
        // Campos de ProductoCientifico
        // Generar código de texto para descripción si se proporciona
        if (dto.getDescripcion() != null && !dto.getDescripcion().isEmpty()) {
            String codigoDescripcion = textosService.createTextInBothLanguages(dto.getDescripcion(), 2);
            publicacion.setDescripcion(codigoDescripcion); // Guardar el código en idDescripcion
        }
        // Generar código de texto para comentario si se proporciona
        if (dto.getComentario() != null && !dto.getComentario().isEmpty()) {
            String codigoComentario = textosService.createTextInBothLanguages(dto.getComentario(), 2);
            publicacion.setComentario(codigoComentario); // Guardar el código en idComentario
        }
        // fechaInicio es obligatorio
        if (dto.getFechaInicio() == null || dto.getFechaInicio().isEmpty()) {
            throw new IllegalArgumentException("fechaInicio is required");
        }
        publicacion.setFechaInicio(parseLocalDate(dto.getFechaInicio()));
        if (dto.getFechaTermino() != null) {
            publicacion.setFechaTermino(parseLocalDate(dto.getFechaTermino()));
        }
        publicacion.setUrlDocumento(dto.getUrlDocumento());
        publicacion.setLinkVisualizacion(dto.getLinkVisualizacion());
        publicacion.setLinkPDF(dto.getLinkPDF());
        publicacion.setProgressReport(dto.getProgressReport());
        publicacion.setCodigoANID(dto.getCodigoANID());
        // Manejar basal: debe ser "S" o "N"
        if (dto.getBasal() != null && !dto.getBasal().isEmpty()) {
            char basalValue = dto.getBasal().charAt(0);
            // Validar que sea S o N
            if (basalValue == 'S' || basalValue == 's' || basalValue == 'N' || basalValue == 'n') {
                publicacion.setBasal(Character.toUpperCase(basalValue));
        } else {
            // Si no es válido, establecer por defecto "S"
            publicacion.setBasal('S');
        }
    } else {
        // Si viene null o vacío, establecer por defecto "S"
        publicacion.setBasal('S');
    }
        publicacion.setLineasInvestigacion(dto.getLineasInvestigacion());
        publicacion.setCluster(dto.getCluster());
        
        // Relaciones de ProductoCientifico
        if (dto.getTipoProducto() != null && dto.getTipoProducto().getId() != null) {
            tipoProductoRepository.findById(dto.getTipoProducto().getId())
                .ifPresent(publicacion::setTipoProducto);
        }
        
        if (dto.getEstadoProducto() != null && dto.getEstadoProducto().getId() != null) {
            estadoProductoRepository.findById(dto.getEstadoProducto().getId())
                .ifPresent(publicacion::setEstadoProducto);
        }
        
        // Campos específicos de Publicacion
        if (dto.getJournal() != null && dto.getJournal().getId() != null) {
            journalRepository.findById(dto.getJournal().getId())
                .ifPresent(publicacion::setJournal);
        }
        publicacion.setVolume(dto.getVolume());
        publicacion.setYearPublished(dto.getYearPublished());
        publicacion.setFirstpage(dto.getFirstpage());
        publicacion.setLastpage(dto.getLastpage());
        publicacion.setIndexs(dto.getIndexs());
        publicacion.setFunding(dto.getFunding());
        publicacion.setDoi(dto.getDoi());
        publicacion.setNumCitas(dto.getNumCitas());
        
        return publicacion;
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
     * Guarda los participantes de una publicación
     */
    private void saveParticipantes(Publicacion publicacion, List<ParticipanteDTO> participantesDTO) {
        for (ParticipanteDTO pDTO : participantesDTO) {
            if (pDTO.getRrhhId() == null || pDTO.getTipoParticipacionId() == null) {
                continue; // Saltar si faltan datos requeridos
            }
            
            RRHH rrhh = rrhhRepository.findById(pDTO.getRrhhId()).orElse(null);
            TipoParticipacion tipoParticipacion = tipoParticipacionRepository.findById(pDTO.getTipoParticipacionId()).orElse(null);
            
            if (rrhh != null && tipoParticipacion != null) {
                // Obtener el siguiente ID correlativo para esta combinación (idProducto, idRRHH)
                Long nextId = participacionProductoRepository.getNextIdForParticipacion(
                    publicacion.getId(), 
                    rrhh.getId()
                );
                
                ParticipacionProducto participacion = new ParticipacionProducto();
                participacion.setRrhh(rrhh);
                participacion.setProducto(publicacion);
                participacion.setTipoParticipacion(tipoParticipacion);
                participacion.setOrden(pDTO.getOrden() != null ? pDTO.getOrden() : 0);
                participacion.setCorresponding(pDTO.getCorresponding() != null && pDTO.getCorresponding());
                
                // Establecer el ID compuesto con el correlativo
                ParticipacionProductoId id = new ParticipacionProductoId(
                    rrhh.getId(), 
                    publicacion.getId(), 
                    nextId
                );
                participacion.setId(id);
                
                participacionProductoRepository.save(participacion);
                
                // Si vienen afiliaciones desde el frontend, recrearlas para esta participación
                if (pDTO.getAfiliaciones() != null && !pDTO.getAfiliaciones().isEmpty()) {
                    for (AfiliacionDTO affDTO : pDTO.getAfiliaciones()) {
                        Long nextAffId = afiliacionRepository.getNextIdForAfiliacion(
                            publicacion.getId(),
                            rrhh.getId(),
                            nextId
                        );
                        
                        AfiliacionId afiliacionId = new AfiliacionId(
                            rrhh.getId(),
                            publicacion.getId(),
                            nextId,
                            nextAffId
                        );
                        
                        Afiliacion afiliacion = new Afiliacion();
                        afiliacion.setId(afiliacionId);
                        afiliacion.setParticipacionProducto(participacion);
                        
                        Institucion institucion = null;
                        if (affDTO.getIdInstitucion() != null) {
                            institucion = institucionRepository.findById(affDTO.getIdInstitucion())
                                .orElse(null);
                        }
                        afiliacion.setInstitucion(institucion);
                        afiliacion.setTexto(affDTO.getTexto());
                        
                        afiliacionRepository.save(afiliacion);
                    }
                }
            }
        }
    }
    
    // ==================== ENDPOINTS PARA AFILIACIONES ====================
    
    /**
     * Obtiene todas las afiliaciones de un participante en una publicación
     */
    @GetMapping("/{publicationId}/participants/{rrhhId}/affiliations")
    @Transactional(readOnly = true)
    public ResponseEntity<List<AfiliacionDTO>> getAfiliaciones(
            @PathVariable Long publicationId,
            @PathVariable Long rrhhId,
            @RequestParam Long rrhhProductoId) {
        
        List<Afiliacion> afiliaciones = afiliacionRepository.findByParticipacion(
            rrhhId, publicationId, rrhhProductoId
        );
        
        List<AfiliacionDTO> afiliacionesDTO = afiliaciones.stream()
            .map(this::convertToAfiliacionDTO)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(afiliacionesDTO);
    }
    
    /**
     * Crea una nueva afiliación para un participante
     */
    @PostMapping("/{publicationId}/participants/{rrhhId}/affiliations")
    @Transactional
    public ResponseEntity<AfiliacionDTO> createAfiliacion(
            @PathVariable Long publicationId,
            @PathVariable Long rrhhId,
            @RequestBody AfiliacionDTO dto) {
        
        // Validar que la participación existe
        ParticipacionProductoId participacionId = new ParticipacionProductoId(
            rrhhId, publicationId, dto.getIdRRHHProducto()
        );
        ParticipacionProducto participacion = participacionProductoRepository.findById(participacionId)
            .orElseThrow(() -> new IllegalArgumentException("Participación no encontrada"));
        
        // Obtener el siguiente ID correlativo
        Long nextId = afiliacionRepository.getNextIdForAfiliacion(
            publicationId, rrhhId, dto.getIdRRHHProducto()
        );
        
        // Crear la afiliación
        AfiliacionId afiliacionId = new AfiliacionId(
            rrhhId, publicationId, dto.getIdRRHHProducto(), nextId
        );
        
        Institucion institucion = null;
        if (dto.getIdInstitucion() != null) {
            institucion = institucionRepository.findById(dto.getIdInstitucion())
                .orElse(null);
        }
        
        Afiliacion afiliacion = new Afiliacion();
        afiliacion.setId(afiliacionId);
        afiliacion.setParticipacionProducto(participacion);
        afiliacion.setInstitucion(institucion);
        afiliacion.setTexto(dto.getTexto());
        
        Afiliacion saved = afiliacionRepository.save(afiliacion);
        return ResponseEntity.ok(convertToAfiliacionDTO(saved));
    }
    
    /**
     * Actualiza una afiliación existente
     */
    @PutMapping("/{publicationId}/participants/{rrhhId}/affiliations/{id}")
    @Transactional
    public ResponseEntity<AfiliacionDTO> updateAfiliacion(
            @PathVariable Long publicationId,
            @PathVariable Long rrhhId,
            @PathVariable Long id,
            @RequestParam Long rrhhProductoId,
            @RequestBody AfiliacionDTO dto) {
        
        AfiliacionId afiliacionId = new AfiliacionId(
            rrhhId, publicationId, rrhhProductoId, id
        );
        
        Afiliacion afiliacion = afiliacionRepository.findById(afiliacionId)
            .orElseThrow(() -> new IllegalArgumentException("Afiliación no encontrada"));
        
        // Actualizar institución
        if (dto.getIdInstitucion() != null) {
            Institucion institucion = institucionRepository.findById(dto.getIdInstitucion())
                .orElse(null);
            afiliacion.setInstitucion(institucion);
        } else {
            afiliacion.setInstitucion(null);
        }
        
        // Actualizar texto
        afiliacion.setTexto(dto.getTexto());
        
        Afiliacion updated = afiliacionRepository.save(afiliacion);
        return ResponseEntity.ok(convertToAfiliacionDTO(updated));
    }
    
    /**
     * Elimina una afiliación
     */
    @DeleteMapping("/{publicationId}/participants/{rrhhId}/affiliations/{id}")
    @Transactional
    public ResponseEntity<Void> deleteAfiliacion(
            @PathVariable Long publicationId,
            @PathVariable Long rrhhId,
            @PathVariable Long id,
            @RequestParam Long rrhhProductoId) {
        
        AfiliacionId afiliacionId = new AfiliacionId(
            rrhhId, publicationId, rrhhProductoId, id
        );
        
        if (afiliacionRepository.existsById(afiliacionId)) {
            afiliacionRepository.deleteById(afiliacionId);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Obtiene el preview de una publicación desde OpenAlex usando su DOI
     * NO persiste nada en la base de datos, solo devuelve los datos parseados con estados de matching
     * 
     * @param doi El DOI de la publicación (con o sin prefijo https://doi.org/)
     * @return PublicationPreviewDTO con todos los datos parseados y estados (matched/new/review)
     */
    @GetMapping("/preview")
    public ResponseEntity<?> getPublicationPreview(@RequestParam String doi) {
        try {
            // Normalizar DOI (quitar https://doi.org/ si viene)
            String normalizedDoi = normalizeDoi(doi);
            
            // 1. Primero verificar si ya existe una publicación con este DOI
            Optional<Publicacion> existingPublication = publicacionRepository.findByDoi(normalizedDoi);
            if (existingPublication.isPresent()) {
                Publicacion pub = existingPublication.get();
                
                // Verificar si el usuario actual puede ver esta publicación
                Long idRRHH = userService.getCurrentUserIdRRHH().orElse(null);
                String userName = userService.getCurrentUsername().orElse(null);
                Optional<Publicacion> visiblePublication = publicacionRepository.findVisibleByIdAndUserIdRRHH(
                    pub.getId(), idRRHH, userName
                );
                
                // Construir respuesta con información de la publicación existente
                Map<String, Object> response = new HashMap<>();
                response.put("exists", true);
                
                if (visiblePublication.isPresent()) {
                    // El usuario puede ver la publicación, incluir el ID
                    response.put("publicationId", pub.getId());
                    response.put("visible", true);
                    
                    // Obtener título desde textos
                    String title = pub.getDescripcion();
                    if (title != null && !title.isEmpty()) {
                        Optional<String> titleText = textosService.getTextValue(title, 2, "us");
                        if (titleText.isPresent()) {
                            title = titleText.get();
                        }
                    }
                    response.put("title", title != null ? title : "N/A");
                    
                    // Obtener journal con descripción
                    String journalName = "N/A";
                    if (pub.getJournal() != null) {
                        Optional<JournalDTO> journalDTO = journalRepository.findByIdWithDescription(
                            pub.getJournal().getId(), "us"
                        );
                        if (journalDTO.isPresent() && journalDTO.get().getDescripcion() != null) {
                            journalName = journalDTO.get().getDescripcion();
                        } else if (pub.getJournal().getAbbreviation() != null) {
                            journalName = pub.getJournal().getAbbreviation();
                        }
                    }
                    response.put("journal", journalName);
                    
                    // Año de publicación
                    response.put("year", pub.getYearPublished() != null ? pub.getYearPublished() : "N/A");
                } else {
                    // El usuario no puede ver la publicación
                    response.put("visible", false);
                    response.put("message", "This publication already exists but is not available for your account.");
                }
                
                return ResponseEntity.ok(response);
            }
            
            // 2. Si no existe, obtener preview desde OpenAlex
            PublicationPreviewDTO preview = openAlexService.getPreviewByDoi(doi);
            Map<String, Object> response = new HashMap<>();
            response.put("exists", false);
            response.put("preview", preview);
            return ResponseEntity.ok(response);
            
        } catch (java.io.FileNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                    "error", "DOI not found in OpenAlex",
                    "message", e.getMessage()
                ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "error", "Error loading publication preview from DOI",
                    "message", e.getMessage()
                ));
        }
    }
    
    /**
     * Normaliza un DOI removiendo el prefijo https://doi.org/ si existe
     */
    private String normalizeDoi(String doi) {
        if (doi == null || doi.trim().isEmpty()) {
            return doi;
        }
        String normalized = doi.trim();
        if (normalized.startsWith("https://doi.org/")) {
            normalized = normalized.substring(16);
        } else if (normalized.startsWith("http://doi.org/")) {
            normalized = normalized.substring(15);
        } else if (normalized.startsWith("doi.org/")) {
            normalized = normalized.substring(8);
        }
        return normalized;
    }
    
    /**
     * Importa una publicación desde DOI con las decisiones del usuario
     * Crea RRHH nuevos, instituciones nuevas, journal si es necesario, y la publicación completa
     */
    @PostMapping("/import-from-doi")
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<?> importPublicationFromDoi(@RequestBody PublicationImportRequestDTO request) {
        try {
            // Log del request recibido para debugging
            System.out.println("=== Import Publication From DOI Request ===");
            System.out.println("Publication: " + (request.getPublication() != null ? request.getPublication().getTitle() : "null"));
            System.out.println("Journal action: " + (request.getJournal() != null ? request.getJournal().getAction() : "null"));
            System.out.println("Authors count: " + (request.getAuthors() != null ? request.getAuthors().size() : 0));
            if (request.getAuthors() != null) {
                for (int i = 0; i < request.getAuthors().size(); i++) {
                    AuthorImportDecisionDTO author = request.getAuthors().get(i);
                    System.out.println("  Author " + i + ": " + author.getName() + 
                        ", action: " + author.getAction() + 
                        ", rrhhId: " + author.getRrhhId() +
                        ", affiliations count: " + (author.getAffiliations() != null ? author.getAffiliations().size() : 0));
                    if (author.getAffiliations() != null) {
                        for (int j = 0; j < author.getAffiliations().size(); j++) {
                            AffiliationImportDecisionDTO aff = author.getAffiliations().get(j);
                            System.out.println("    Affiliation " + j + ": " + aff.getName() + 
                                ", action: " + aff.getAction() + 
                                ", institutionId: " + aff.getInstitutionId());
                        }
                    }
                }
            }
            System.out.println("===========================================");
            
            // 1. Validar que todas las decisiones estén tomadas
            validateImportRequest(request);
            
            // 2. Procesar journal
            Journal journal = processJournal(request.getJournal());
            
            // 3. Procesar autores (crear RRHH nuevos si es necesario)
            // Esto retorna un mapa de openAlexId -> rrhhId final
            java.util.Map<String, Long> authorRrhhIdMap = processAuthors(request.getAuthors());
            
            // 4. Crear la publicación
            PublicacionDTO pubDTO = buildPublicationDTO(request, journal);
            Publicacion publicacion = convertFromDTO(pubDTO);
            Publicacion saved = publicacionRepository.save(publicacion);
            
            // 5. Crear participantes y afiliaciones
            saveAuthorsWithAffiliations(saved, request.getAuthors(), authorRrhhIdMap);
            
            // 6. Verificar si hay autores sin afiliaciones y establecer el estado
            boolean hasAuthorsWithoutAffiliations = hasAuthorsWithoutAffiliations(saved);
            EstadoProducto estadoProducto;
            if (hasAuthorsWithoutAffiliations) {
                // Si hay autores sin afiliaciones → Publication pending (id = 4)
                estadoProducto = estadoProductoRepository.findById(4L)
                    .orElseThrow(() -> new IllegalStateException("EstadoProducto with id=4 (Publication pending) not found"));
            } else {
                // Si todos los autores tienen afiliaciones → Publication complete (id = 3)
                estadoProducto = estadoProductoRepository.findById(3L)
                    .orElseThrow(() -> new IllegalStateException("EstadoProducto with id=3 (Publication complete) not found"));
            }
            saved.setEstadoProducto(estadoProducto);
            saved = publicacionRepository.save(saved);
            
            // 7. Asignar proyecto por defecto
            assignDefaultProject(saved);
            
            // 8. Retornar la publicación creada
            PublicacionDTO resultDTO = convertToDTO(saved);
            return ResponseEntity.ok(resultDTO);
            
        } catch (IllegalArgumentException e) {
            // Log del error de validación
            System.err.println("Validation error in importPublicationFromDoi: " + e.getMessage());
            e.printStackTrace();
            // Las excepciones RuntimeException hacen rollback automático, pero las marcamos explícitamente
            throw e; // Re-lanzar para que el rollback funcione correctamente
        } catch (UnsupportedOperationException e) {
            // Log del error de operación no soportada
            System.err.println("Unsupported operation in importPublicationFromDoi: " + e.getMessage());
            e.printStackTrace();
            // Re-lanzar para que el rollback funcione correctamente
            throw e;
        } catch (Exception e) {
            // Log detallado del error
            System.err.println("Unexpected error in importPublicationFromDoi: " + e.getMessage());
            e.printStackTrace();
            // Re-lanzar como RuntimeException para asegurar rollback
            throw new RuntimeException("Error importing publication: " + e.getMessage(), e);
        }
    }
    
    /**
     * Valida que todas las decisiones estén tomadas
     */
    private void validateImportRequest(PublicationImportRequestDTO request) {
        if (request.getPublication() == null) {
            throw new IllegalArgumentException("Publication data is required");
        }
        
        // Validar journal
        if (request.getJournal() == null || request.getJournal().getAction() == null) {
            throw new IllegalArgumentException("Journal decision is required");
        }
        if ("link".equals(request.getJournal().getAction()) && request.getJournal().getJournalId() == null) {
            throw new IllegalArgumentException("Journal ID is required when linking to existing journal");
        }
        if ("create".equals(request.getJournal().getAction()) && 
            (request.getJournal().getName() == null || request.getJournal().getName().trim().isEmpty())) {
            throw new IllegalArgumentException("Journal name is required when creating new journal");
        }
        
        // Validar autores
        if (request.getAuthors() == null || request.getAuthors().isEmpty()) {
            throw new IllegalArgumentException("At least one author is required");
        }
        
        for (AuthorImportDecisionDTO author : request.getAuthors()) {
            if (author.getAction() == null) {
                throw new IllegalArgumentException("Author decision is required for: " + author.getName());
            }
            if ("link".equals(author.getAction()) && author.getRrhhId() == null) {
                throw new IllegalArgumentException("RRHH ID is required when linking author: " + author.getName());
            }
            if ("create".equals(author.getAction()) && 
                (author.getName() == null || author.getName().trim().isEmpty())) {
                throw new IllegalArgumentException("Author name is required when creating new researcher: " + author.getName());
            }
            if (author.getTipoParticipacionId() == null) {
                throw new IllegalArgumentException("Participation type is required for author: " + author.getName());
            }
            
            // Validar afiliaciones
            if (author.getAffiliations() != null) {
                for (AffiliationImportDecisionDTO aff : author.getAffiliations()) {
                    if (aff.getAction() == null) {
                        throw new IllegalArgumentException("Affiliation decision is required for author: " + author.getName());
                    }
                    if ("link".equals(aff.getAction()) && aff.getInstitutionId() == null) {
                        throw new IllegalArgumentException("Institution ID is required when linking affiliation for author: " + author.getName());
                    }
                    // Para "create", por ahora solo validamos que tenga nombre (la creación la haremos después)
                    if ("create".equals(aff.getAction()) && 
                        (aff.getName() == null || aff.getName().trim().isEmpty())) {
                        throw new IllegalArgumentException("Institution name is required when creating new institution for author: " + author.getName());
                    }
                }
            }
        }
    }
    
    /**
     * Procesa el journal según la decisión del usuario
     */
    private Journal processJournal(JournalImportDecisionDTO journalDecision) {
        if ("matched".equals(journalDecision.getAction()) && journalDecision.getJournalId() != null) {
            // Ya está matched, solo retornar el journal existente
            return journalRepository.findById(journalDecision.getJournalId())
                .orElseThrow(() -> new IllegalArgumentException("Journal not found: " + journalDecision.getJournalId()));
        } else if ("link".equals(journalDecision.getAction()) && journalDecision.getJournalId() != null) {
            // Linkear a journal existente
            return journalRepository.findById(journalDecision.getJournalId())
                .orElseThrow(() -> new IllegalArgumentException("Journal not found: " + journalDecision.getJournalId()));
        } else if ("create".equals(journalDecision.getAction())) {
            // Validar que se proporcione el nombre del journal
            if (journalDecision.getName() == null || journalDecision.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("Journal name is required when creating a new journal");
            }
            
            // Crear nuevo journal
            // Crear texto en ambos idiomas (us y es) usando TextosService
            // idTipoTexto = 2 para descripciones/comentarios
            String codigoTexto = textosService.createTextInBothLanguages(
                journalDecision.getName().trim(), 
                2
            );
            
            // Verificar si ya existe un journal con el mismo código de texto (evitar duplicados)
            // Nota: Esta verificación es básica. Para una validación más robusta, se podría
            // verificar también por ISSN o nombre normalizado
            java.util.Optional<JournalDTO> existingJournalDTO = journalRepository.findAllByLanguage("us").stream()
                .filter(j -> codigoTexto.equals(j.getIdDescripcion()))
                .findFirst();
            
            if (existingJournalDTO.isPresent()) {
                // Si ya existe, retornar el existente en lugar de crear uno nuevo
                JournalDTO existingDTO = existingJournalDTO.get();
                java.util.Optional<Journal> existingJournal = journalRepository.findById(existingDTO.getId());
                if (existingJournal.isPresent()) {
                    System.out.println("Journal with code " + codigoTexto + " already exists. Returning existing journal.");
                    return existingJournal.get();
                }
            }
            
            Journal newJournal = new Journal();
            newJournal.setIdDescripcion(codigoTexto); // Usar el código de texto directamente como String
            newJournal.setIssn(journalDecision.getIssn() != null ? journalDecision.getIssn().trim() : null);
            // abbreviation puede quedar null por ahora
            Journal saved = journalRepository.save(newJournal);
            
            System.out.println("Created new journal: ID=" + saved.getId() + ", idDescripcion=" + codigoTexto + ", name=" + journalDecision.getName());
            return saved;
        } else {
            throw new IllegalArgumentException("Invalid journal action: " + journalDecision.getAction());
        }
    }
    
    /**
     * Procesa los autores y crea RRHH nuevos si es necesario
     * Retorna un mapa de openAlexId -> rrhhId final
     */
    private java.util.Map<String, Long> processAuthors(List<AuthorImportDecisionDTO> authors) {
        java.util.Map<String, Long> rrhhIdMap = new java.util.HashMap<>();
        
        for (AuthorImportDecisionDTO author : authors) {
            Long finalRrhhId;
            
            if ("matched".equals(author.getAction()) && author.getRrhhId() != null) {
                // Ya está matched
                finalRrhhId = author.getRrhhId();
            } else if ("link".equals(author.getAction()) && author.getRrhhId() != null) {
                // Linkear a RRHH existente
                finalRrhhId = author.getRrhhId();
            } else if ("create".equals(author.getAction())) {
                // Validar que se proporcione el nombre del autor
                if (author.getName() == null || author.getName().trim().isEmpty()) {
                    throw new IllegalArgumentException("Author name is required when creating new researcher: " + author.getName());
                }
                
                // Crear nuevo RRHH con idTipoRRHH=32 (investigador externo)
                // El método createResearcher ya maneja la creación con idTipoRRHH=32
                finalRrhhId = researcherMatchingService.createResearcher(
                    author.getName().trim(), 
                    author.getOrcid() != null ? author.getOrcid().trim() : null
                );
                
                System.out.println("Created new researcher: ID=" + finalRrhhId + ", name=" + author.getName());
            } else {
                throw new IllegalArgumentException("Invalid author action or missing data for: " + author.getName());
            }
            
            if (author.getOpenAlexId() != null) {
                rrhhIdMap.put(author.getOpenAlexId(), finalRrhhId);
            }
        }
        
        return rrhhIdMap;
    }
    
    /**
     * Construye un PublicacionDTO a partir del request
     */
    private PublicacionDTO buildPublicationDTO(PublicationImportRequestDTO request, Journal journal) {
        PublicationPreviewDataDTO pubData = request.getPublication();
        
        // Validaciones básicas
        if (pubData.getTitle() == null || pubData.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Publication title is required");
        }
        if (pubData.getPublicationYear() == null || pubData.getPublicationYear() < 1900 || pubData.getPublicationYear() > 2100) {
            throw new IllegalArgumentException("Valid publication year is required (1900-2100)");
        }
        if (journal == null) {
            throw new IllegalArgumentException("Journal is required");
        }
        
        PublicacionDTO dto = new PublicacionDTO();
        
        // Datos básicos
        dto.setDescripcion(pubData.getTitle().trim());
        dto.setComentario(pubData.getDisplayName() != null ? pubData.getDisplayName().trim() : null);
        dto.setFechaInicio(pubData.getPublicationDate());
        dto.setDoi(pubData.getDoi() != null ? pubData.getDoi().trim() : null);
        dto.setLinkVisualizacion(pubData.getDoi() != null ? "https://doi.org/" + pubData.getDoi().trim() : null);
        dto.setProgressReport(openAlexService.calculateProgressReport(pubData.getPublicationDate()));
        dto.setBasal("N"); // Por defecto "N"
        
        // Datos específicos de publicación
        if (journal != null) {
            // Obtener journal con descripción resuelta desde textos
            JournalDTO journalDTO = journalRepository.findByIdWithDescription(
                journal.getId(), 
                "us"
            ).orElseGet(() -> {
                // Fallback si no se encuentra con descripción
                JournalDTO fallback = new JournalDTO();
                fallback.setId(journal.getId());
                fallback.setIdDescripcion(journal.getIdDescripcion());
                fallback.setAbbreviation(journal.getAbbreviation());
                fallback.setIssn(journal.getIssn());
                fallback.setCreatedAt(journal.getCreatedAt());
                fallback.setUpdatedAt(journal.getUpdatedAt());
                return fallback;
            });
            dto.setJournal(journalDTO);
        }
        dto.setVolume(pubData.getVolume());
        dto.setYearPublished(pubData.getPublicationYear());
        dto.setFirstpage(pubData.getFirstPage());
        dto.setLastpage(pubData.getLastPage());
        dto.setIndexs("[]"); // Vacío por ahora
        dto.setFunding("[]"); // Vacío por ahora
        
        // Tipo de producto: 3 = Publicaciones
        TipoProductoDTO tipoProducto = new TipoProductoDTO();
        tipoProducto.setId(3L);
        dto.setTipoProducto(tipoProducto);
        
        // Estado por defecto (necesito ver qué estado usar)
        // Por ahora usar el primero disponible o un estado por defecto
        
        return dto;
    }
    
    /**
     * Guarda los autores con sus afiliaciones
     */
    private void saveAuthorsWithAffiliations(Publicacion publicacion, 
                                             List<AuthorImportDecisionDTO> authors, 
                                             java.util.Map<String, Long> rrhhIdMap) {
        for (AuthorImportDecisionDTO authorDTO : authors) {
            // Obtener el RRHH ID final
            Long rrhhIdTemp = null;
            if (authorDTO.getOpenAlexId() != null) {
                rrhhIdTemp = rrhhIdMap.get(authorDTO.getOpenAlexId());
            }
            if (rrhhIdTemp == null && authorDTO.getRrhhId() != null) {
                rrhhIdTemp = authorDTO.getRrhhId();
            }
            
            if (rrhhIdTemp == null) {
                throw new IllegalArgumentException("RRHH ID not found for author: " + authorDTO.getName());
            }
            
            final Long rrhhId = rrhhIdTemp;
            
            RRHH rrhh = rrhhRepository.findById(rrhhId)
                .orElseThrow(() -> new IllegalArgumentException("RRHH not found: " + rrhhId));
            
            TipoParticipacion tipoParticipacion = tipoParticipacionRepository.findById(authorDTO.getTipoParticipacionId())
                .orElseThrow(() -> new IllegalArgumentException("TipoParticipacion not found: " + authorDTO.getTipoParticipacionId()));
            
            // Obtener el siguiente ID correlativo
            Long nextId = participacionProductoRepository.getNextIdForParticipacion(
                publicacion.getId(), 
                rrhhId
            );
            
            // Crear participación
            ParticipacionProducto participacion = new ParticipacionProducto();
            participacion.setRrhh(rrhh);
            participacion.setProducto(publicacion);
            participacion.setTipoParticipacion(tipoParticipacion);
            participacion.setOrden(authorDTO.getOrder() != null ? authorDTO.getOrder() : 0);
            participacion.setCorresponding(authorDTO.getIsCorresponding() != null && authorDTO.getIsCorresponding());
            
            ParticipacionProductoId participacionId = new ParticipacionProductoId(
                rrhhId, 
                publicacion.getId(), 
                nextId
            );
            participacion.setId(participacionId);
            
            ParticipacionProducto savedParticipacion = participacionProductoRepository.save(participacion);
            
            // Crear afiliaciones
            if (authorDTO.getAffiliations() != null && !authorDTO.getAffiliations().isEmpty()) {
                saveAffiliationsForAuthor(publicacion, savedParticipacion, authorDTO.getAffiliations());
            }
        }
    }
    
    /**
     * Guarda las afiliaciones de un autor
     */
    private void saveAffiliationsForAuthor(Publicacion publicacion,
                                           ParticipacionProducto participacion,
                                           List<AffiliationImportDecisionDTO> affiliations) {
        for (AffiliationImportDecisionDTO affDTO : affiliations) {
            // Obtener el siguiente ID correlativo para la afiliación
            Long nextAffId = afiliacionRepository.getNextIdForAfiliacion(
                publicacion.getId(),
                participacion.getId().getRrhhId(),
                participacion.getId().getId()
            );
            
            AfiliacionId afiliacionId = new AfiliacionId(
                participacion.getId().getRrhhId(),
                publicacion.getId(),
                participacion.getId().getId(),
                nextAffId
            );
            
            Afiliacion afiliacion = new Afiliacion();
            afiliacion.setId(afiliacionId);
            afiliacion.setParticipacionProducto(participacion);
            afiliacion.setTexto(affDTO.getTexto());
            
            // Procesar institución
            if ("matched".equals(affDTO.getAction()) || "link".equals(affDTO.getAction())) {
                if (affDTO.getInstitutionId() != null) {
                    Institucion institucion = institucionRepository.findById(affDTO.getInstitutionId())
                        .orElse(null);
                    afiliacion.setInstitucion(institucion);
                }
            } else if ("create".equals(affDTO.getAction())) {
                // Por ahora, crear institución nueva (esto lo mejoraremos después)
                // Por ahora lanzamos un error indicando que no está implementado
                throw new UnsupportedOperationException(
                    "Creating new institutions is not yet implemented. Please link to an existing institution for: " + affDTO.getName()
                );
            }
            
            afiliacionRepository.save(afiliacion);
        }
    }
    
    /**
     * Asigna el proyecto por defecto a la publicación
     */
    private void assignDefaultProject(Publicacion publicacion) {
        String defaultProjectCode = openAlexService.getDefaultProjectCode();
        if (defaultProjectCode != null && !defaultProjectCode.trim().isEmpty()) {
            proyectoRepository.findByCodigo(defaultProjectCode).ifPresent(proyecto -> {
                // Verificar si ya existe la relación
                ProductoProyectoId id = new ProductoProyectoId(publicacion.getId(), proyecto.getCodigo());
                if (!productoProyectoRepository.existsById(id)) {
                    // Crear la relación
                    ProductoProyecto productoProyecto = new ProductoProyecto(publicacion, proyecto);
                    productoProyectoRepository.save(productoProyecto);
                }
            });
        }
    }
    
    /**
     * Convierte una entidad Afiliacion a DTO
     */
    private AfiliacionDTO convertToAfiliacionDTO(Afiliacion afiliacion) {
        AfiliacionDTO dto = new AfiliacionDTO();
        dto.setIdRRHH(afiliacion.getId().getIdRRHH());
        dto.setIdProducto(afiliacion.getId().getIdProducto());
        dto.setIdRRHHProducto(afiliacion.getId().getIdRRHHProducto());
        dto.setId(afiliacion.getId().getId());
        
        if (afiliacion.getInstitucion() != null) {
            dto.setIdInstitucion(afiliacion.getInstitucion().getId());
            dto.setNombreInstitucion(afiliacion.getInstitucion().getDescripcion());
        }
        
        dto.setTexto(afiliacion.getTexto());
        
        return dto;
    }
    
    /**
     * Verifica si hay autores sin afiliaciones en la publicación
     * @param publicacion La publicación a verificar
     * @return true si hay al menos un autor sin afiliaciones, false si todos tienen afiliaciones
     */
    private boolean hasAuthorsWithoutAffiliations(Publicacion publicacion) {
        // Obtener todos los participantes de la publicación
        List<ParticipacionProducto> participantes = participacionProductoRepository
            .findByProductoId(publicacion.getId());
        
        if (participantes == null || participantes.isEmpty()) {
            return false; // Si no hay participantes, no hay problema
        }
        
        // Verificar cada participante
        for (ParticipacionProducto participante : participantes) {
            Long rrhhId = participante.getId().getRrhhId();
            Long productoId = participante.getId().getProductoId();
            Long rrhhProductoId = participante.getId().getId();
            
            // Buscar afiliaciones para este participante
            List<Afiliacion> afiliaciones = afiliacionRepository.findByParticipacion(
                rrhhId, productoId, rrhhProductoId
            );
            
            // Si este participante no tiene afiliaciones, retornar true
            if (afiliaciones == null || afiliaciones.isEmpty()) {
                return true;
            }
        }
        
        // Todos los participantes tienen afiliaciones
        return false;
    }
}

