package com.sisgic.controller;

import com.sisgic.dto.TipoParticipacionDTO;
import com.sisgic.dto.CreateTipoParticipacionDTO;
import com.sisgic.dto.TipoProductoDTO;
import com.sisgic.dto.CreateTipoProductoDTO;
import com.sisgic.dto.TipoEventoDTO;
import com.sisgic.dto.PaisDTO;
import com.sisgic.dto.JournalDTO;
import com.sisgic.dto.InstitucionDTO;
import com.sisgic.dto.GradoAcademicoDTO;
import com.sisgic.dto.EstadoTesisDTO;
import com.sisgic.dto.TipoSectorDTO;
import com.sisgic.dto.TipoTransferenciaDTO;
import com.sisgic.dto.CategoriaTransferenciaDTO;
import com.sisgic.dto.ResourceDTO;
import com.sisgic.dto.FundingTypeDTO;
import com.sisgic.dto.TipoDifusionDTO;
import com.sisgic.dto.PublicoObjetivoDTO;
import com.sisgic.dto.TipoColaboracionDTO;
import com.sisgic.dto.TipoRRHHDTO;
import com.sisgic.dto.IndexTypeDTO;
import com.sisgic.entity.TipoParticipacion;
import com.sisgic.entity.TipoProducto;
import com.sisgic.entity.TipoEvento;
import com.sisgic.entity.Pais;
import com.sisgic.entity.Journal;
import com.sisgic.entity.Institucion;
import com.sisgic.entity.GradoAcademico;
import com.sisgic.entity.EstadoTesis;
import com.sisgic.entity.TipoSector;
import com.sisgic.entity.TipoTransferencia;
import com.sisgic.entity.CategoriaTransferencia;
import com.sisgic.entity.Resource;
import com.sisgic.entity.FundingType;
import com.sisgic.entity.TipoDifusion;
import com.sisgic.entity.PublicoObjetivo;
import com.sisgic.entity.TipoColaboracion;
import com.sisgic.entity.VIndexType;
import com.sisgic.repository.TipoParticipacionRepository;
import com.sisgic.repository.TipoProductoRepository;
import com.sisgic.repository.TipoEventoRepository;
import com.sisgic.repository.PaisRepository;
import com.sisgic.repository.JournalRepository;
import com.sisgic.repository.InstitucionRepository;
import com.sisgic.repository.GradoAcademicoRepository;
import com.sisgic.repository.EstadoTesisRepository;
import com.sisgic.repository.TipoSectorRepository;
import com.sisgic.repository.TipoTransferenciaRepository;
import com.sisgic.repository.CategoriaTransferenciaRepository;
import com.sisgic.repository.ResourceRepository;
import com.sisgic.repository.FundingTypeRepository;
import com.sisgic.repository.TipoDifusionRepository;
import com.sisgic.repository.PublicoObjetivoRepository;
import com.sisgic.repository.TipoColaboracionRepository;
import com.sisgic.repository.TipoRRHHRepository;
import com.sisgic.repository.VIndexTypeRepository;
import com.sisgic.entity.TipoRRHH;
import com.sisgic.service.TextosService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/catalogs")
@CrossOrigin(origins = "*", maxAge = 3600)
public class CatalogController {
    
    @Autowired
    private TipoParticipacionRepository tipoParticipacionRepository;
    
    @Autowired
    private TipoProductoRepository tipoProductoRepository;
    
    @Autowired
    private JournalRepository journalRepository;
    
    @Autowired
    private TipoEventoRepository tipoEventoRepository;
    
    @Autowired
    private PaisRepository paisRepository;
    
    @Autowired
    private InstitucionRepository institucionRepository;
    
    @Autowired
    private GradoAcademicoRepository gradoAcademicoRepository;
    
    @Autowired
    private EstadoTesisRepository estadoTesisRepository;
    
    @Autowired
    private TipoSectorRepository tipoSectorRepository;
    
    @Autowired
    private TipoTransferenciaRepository tipoTransferenciaRepository;
    
    @Autowired
    private CategoriaTransferenciaRepository categoriaTransferenciaRepository;
    
    @Autowired
    private ResourceRepository resourceRepository;
    
    @Autowired
    private FundingTypeRepository fundingTypeRepository;
    
    @Autowired
    private TipoDifusionRepository tipoDifusionRepository;
    
    @Autowired
    private PublicoObjetivoRepository publicoObjetivoRepository;
    
    @Autowired
    private TextosService textosService;
    
    @Autowired
    private com.sisgic.service.CatalogService catalogService;

    @Autowired
    private VIndexTypeRepository vIndexTypeRepository;
    
    @GetMapping("/participation-types")
    public ResponseEntity<List<TipoParticipacionDTO>> getAllParticipationTypes() {
        System.out.println("CatalogController: GET /api/catalogs/participation-types - usando vista");
        List<TipoParticipacion> results = tipoParticipacionRepository.findAllByOrderByIdAsc();
        
        System.out.println("CatalogController: Encontrados " + results.size() + " registros de la vista");
        for (TipoParticipacion tipo : results) {
            System.out.println("  - ID: " + tipo.getId() + ", Descripcion: " + tipo.getDescripcion() + ", TipoProductoNombre: " + tipo.getTipoProductoNombre());
        }
        
        List<TipoParticipacionDTO> dtos = results.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
            
        /*System.out.println("CatalogController: Retornando " + dtos.size() + " tipos de participación");
        for (TipoParticipacionDTO dto : dtos) {
            System.out.println("  - DTO ID: " + dto.getId() + ", Descripcion: " + dto.getDescripcion() + ", TipoProductoNombre: " + dto.getTipoProductoNombre());
        }*/
        return ResponseEntity.ok(dtos);
    }
    
    @GetMapping("/participation-types/{id}")
    public ResponseEntity<TipoParticipacionDTO> getParticipationTypeById(@PathVariable Long id) {
        System.out.println("CatalogController: GET /api/catalogs/participation-types/" + id + " - usando vista");
        return tipoParticipacionRepository.findById(id)
            .map(tipo -> {
                System.out.println("CatalogController: Encontrado tipo de participación: " + tipo.getDescripcion());
                return ResponseEntity.ok(convertToDTO(tipo));
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/participation-types/by-product-type/{idTipoProducto}")
    public ResponseEntity<List<TipoParticipacionDTO>> getParticipationTypesByProductType(
            @PathVariable Long idTipoProducto) {
        
        // Buscar por tipo de producto usando JPQL
        List<TipoParticipacion> results = tipoParticipacionRepository.findByIdTipoProductoOrderByIdAsc(idTipoProducto);
        
        // Si JPQL no funciona, intentar con query nativa como fallback
        if (results.isEmpty()) {
            long countNative = tipoParticipacionRepository.countByIdTipoProductoNative(idTipoProducto);
            if (countNative > 0) {
                System.out.println("CatalogController: JPQL no retornó resultados pero hay " + countNative + " registros. Intentando con query nativa...");
                results = tipoParticipacionRepository.findByIdTipoProductoOrderByIdAscNative(idTipoProducto);
            }
        }
        
        List<TipoParticipacionDTO> dtos = results.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
            
        return ResponseEntity.ok(dtos);
    }
    
    @PostMapping("/participation-types")
    public ResponseEntity<TipoParticipacionDTO> createParticipationType(@RequestBody CreateTipoParticipacionDTO dto) {
        // Crear texto en ambos idiomas
        String codigoTexto = textosService.createTextInBothLanguages(dto.getDescripcion(), 2);
        
        // Obtener nombre del tipo de producto
        String tipoProductoNombre = tipoProductoRepository.findById(dto.getIdTipoProducto())
            .map(tipoProducto -> tipoProducto.getDescripcion())
            .orElse("Unknown");
        
        // Crear tipo de participación
        TipoParticipacion tipo = new TipoParticipacion(codigoTexto, dto.getIdTipoProducto(), dto.getDescripcion(), tipoProductoNombre);
        TipoParticipacion saved = tipoParticipacionRepository.save(tipo);
        
        // Calcular aplicableProductos
        String aplicableProductos = "ALL";
        if (saved.getIdTipoProducto() != null && tipoProductoNombre != null && !tipoProductoNombre.isEmpty()) {
            aplicableProductos = tipoProductoNombre.toUpperCase();
        }
        
        TipoParticipacionDTO response = new TipoParticipacionDTO(
            saved.getId(),
            saved.getIdDescripcion(),
            saved.getIdTipoProducto(),
            dto.getDescripcion(),
            tipoProductoNombre,
            aplicableProductos
        );
        
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/participation-types/{id}")
    public ResponseEntity<TipoParticipacionDTO> updateParticipationType(@PathVariable Long id, @RequestBody CreateTipoParticipacionDTO dto) {
        return tipoParticipacionRepository.findById(id)
            .map(existing -> {
                // Actualizar texto en ambos idiomas
                textosService.updateTextInBothLanguages(existing.getIdDescripcion(), dto.getDescripcion(), 2);
                
                // Actualizar tipo de producto si cambió
                existing.setIdTipoProducto(dto.getIdTipoProducto());
                TipoParticipacion saved = tipoParticipacionRepository.save(existing);
                
                // Obtener nombre del tipo de producto
                String tipoProductoNombre = tipoProductoRepository.findById(dto.getIdTipoProducto())
                    .map(tipoProducto -> textosService.getTextValue(tipoProducto.getIdDescripcion(), 2, "us").orElse("Unknown"))
                    .orElse("Unknown");
                
                // Calcular aplicableProductos
                String aplicableProductos = "ALL";
                if (saved.getIdTipoProducto() != null && tipoProductoNombre != null && !tipoProductoNombre.isEmpty()) {
                    aplicableProductos = tipoProductoNombre.toUpperCase();
                }
                
                TipoParticipacionDTO response = new TipoParticipacionDTO(
                    saved.getId(),
                    saved.getIdDescripcion(),
                    saved.getIdTipoProducto(),
                    dto.getDescripcion(),
                    tipoProductoNombre,
                    aplicableProductos
                );
                
                return ResponseEntity.ok(response);
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/participation-types/{id}")
    public ResponseEntity<Void> deleteParticipationType(@PathVariable Long id) {
        return tipoParticipacionRepository.findById(id)
            .map(existing -> {
                // Eliminar texto de ambos idiomas
                textosService.deleteTextFromBothLanguages(existing.getIdDescripcion(), 2);
                
                // Eliminar tipo de participación
                tipoParticipacionRepository.deleteById(id);
                return ResponseEntity.ok().<Void>build();
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    private TipoParticipacionDTO convertArrayToDTO(Object[] result) {
        Long id = (Long) result[0];
        String idDescripcion = (String) result[1];
        Long idTipoProducto = (Long) result[2];
        String descripcion = (String) result[3];
        
        // Obtener nombre del tipo de producto
        String tipoProductoNombre = tipoProductoRepository.findById(idTipoProducto)
            .map(tipoProducto -> tipoProducto.getDescripcion())
            .orElse("Unknown");
        
        return new TipoParticipacionDTO(id, idDescripcion, idTipoProducto, descripcion, tipoProductoNombre);
    }
    
    private TipoParticipacionDTO convertToDTOWithDescription(TipoParticipacion tipo, String descripcion) {
        // Obtener nombre del tipo de producto
        String tipoProductoNombre = tipoProductoRepository.findById(tipo.getIdTipoProducto())
            .map(tipoProducto -> tipoProducto.getDescripcion())
            .orElse("Unknown");
        
        // Calcular aplicableProductos basado en idTipoProducto
        String aplicableProductos = "ALL";
        if (tipo.getIdTipoProducto() != null) {
            if (tipoProductoNombre != null && !tipoProductoNombre.isEmpty()) {
                aplicableProductos = tipoProductoNombre.toUpperCase();
            } else {
                aplicableProductos = "ALL";
            }
        }
        
        return new TipoParticipacionDTO(
            tipo.getId(),
            tipo.getIdDescripcion(),
            tipo.getIdTipoProducto(),
            descripcion,
            tipoProductoNombre,
            aplicableProductos
        );
    }
    
    // ===== PRODUCT TYPES ENDPOINTS =====
    
    @GetMapping("/product-types")
    public ResponseEntity<List<TipoProductoDTO>> getAllProductTypes() {
        System.out.println("CatalogController: GET /api/catalogs/product-types - usando vista");
        List<TipoProducto> results = tipoProductoRepository.findAllByOrderByIdAsc();
        System.out.println("CatalogController: Encontrados " + results.size() + " tipos de producto");
        
        List<TipoProductoDTO> dtos = results.stream()
            .map(this::convertProductToDTO)
            .collect(Collectors.toList());
            
        System.out.println("CatalogController: Retornando " + dtos.size() + " tipos de producto");
        return ResponseEntity.ok(dtos);
    }
    
    @GetMapping("/product-types/{id}")
    public ResponseEntity<TipoProductoDTO> getProductTypeById(@PathVariable Long id) {
        System.out.println("CatalogController: GET /api/catalogs/product-types/" + id + " - usando vista");
        return tipoProductoRepository.findById(id)
            .map(tipo -> {
                System.out.println("CatalogController: Encontrado tipo de producto: " + tipo.getDescripcion());
                return ResponseEntity.ok(convertProductToDTO(tipo));
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/product-types")
    public ResponseEntity<TipoProductoDTO> createProductType(@RequestBody CreateTipoProductoDTO dto) {
        // Crear texto en ambos idiomas
        String codigoTexto = textosService.createTextInBothLanguages(dto.getDescripcion(), 2);
        
        // Crear tipo de producto
        TipoProducto tipo = new TipoProducto(codigoTexto, dto.getDescripcion());
        TipoProducto saved = tipoProductoRepository.save(tipo);
        
        TipoProductoDTO response = new TipoProductoDTO(
            saved.getId(),
            saved.getIdDescripcion(),
            dto.getDescripcion()
        );
        
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/product-types/{id}")
    public ResponseEntity<TipoProductoDTO> updateProductType(@PathVariable Long id, @RequestBody CreateTipoProductoDTO dto) {
        return tipoProductoRepository.findById(id)
            .map(existing -> {
                // Actualizar texto en ambos idiomas
                textosService.updateTextInBothLanguages(existing.getIdDescripcion(), dto.getDescripcion(), 2);
                
                TipoProductoDTO response = new TipoProductoDTO(
                    existing.getId(),
                    existing.getIdDescripcion(),
                    dto.getDescripcion()
                );
                
                return ResponseEntity.ok(response);
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/product-types/{id}")
    public ResponseEntity<Void> deleteProductType(@PathVariable Long id) {
        return tipoProductoRepository.findById(id)
            .map(existing -> {
                // Eliminar texto de ambos idiomas
                textosService.deleteTextFromBothLanguages(existing.getIdDescripcion(), 2);
                
                // Eliminar tipo de producto
                tipoProductoRepository.deleteById(id);
                return ResponseEntity.ok().<Void>build();
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    private TipoProductoDTO convertProductArrayToDTO(Object[] result) {
        Long id = (Long) result[0];
        String idDescripcion = (String) result[1];
        String descripcion = (String) result[2];
        
        return new TipoProductoDTO(id, idDescripcion, descripcion);
    }
    
    private TipoProductoDTO convertToProductDTOWithDescription(TipoProducto tipo, String descripcion) {
        return new TipoProductoDTO(
            tipo.getId(),
            tipo.getIdDescripcion(),
            descripcion
        );
    }
    
    // ===== MÉTODOS DE CONVERSIÓN SIMPLIFICADOS PARA VISTAS =====
    
    private TipoParticipacionDTO convertToDTO(TipoParticipacion tipo) {
        // Calcular aplicableProductos basado en idTipoProducto
        // Si idTipoProducto es null, aplica a todos (ALL)
        // Si tiene un valor, aplica solo a ese tipo de producto
        String aplicableProductos = "ALL"; // Por defecto aplica a todos
        if (tipo.getIdTipoProducto() != null) {
            // Si tiene un tipo de producto específico, usar el nombre del tipo en mayúsculas
            // o mantener "ALL" si queremos que todos los tipos de participación sean aplicables a todos los productos
            // Por ahora, si tiene idTipoProducto, asumimos que aplica solo a ese tipo
            String tipoProductoNombre = tipo.getTipoProductoNombre();
            if (tipoProductoNombre != null && !tipoProductoNombre.isEmpty()) {
                aplicableProductos = tipoProductoNombre.toUpperCase();
            } else {
                // Si no hay nombre, usar "ALL" para que sea compatible con todos
                aplicableProductos = "ALL";
            }
        }
        
        return new TipoParticipacionDTO(
            tipo.getId(),
            tipo.getIdDescripcion(),
            tipo.getIdTipoProducto(),
            tipo.getDescripcion(),
            tipo.getTipoProductoNombre(),
            aplicableProductos
        );
    }
    
    private TipoProductoDTO convertProductToDTO(TipoProducto tipo) {
        return new TipoProductoDTO(
            tipo.getId(),
            tipo.getIdDescripcion(),
            tipo.getDescripcion()
        );
    }
    
    // ========================================
    // JOURNAL ENDPOINTS
    // ========================================
    
    @GetMapping("/journals")
    public ResponseEntity<List<JournalDTO>> getAllJournals() {
        List<JournalDTO> results = catalogService.getAllJournals("us");
        return ResponseEntity.ok(results);
    }
    
    @GetMapping("/journals/{id}")
    public ResponseEntity<JournalDTO> getJournalById(@PathVariable Long id) {
        return catalogService.getJournalById(id, "us")
            .map(journal -> ResponseEntity.ok(journal))
            .orElse(ResponseEntity.notFound().build());
    }

    // ========================================
    // INDEX TYPES ENDPOINTS
    // ========================================

    @GetMapping("/index-types")
    public ResponseEntity<List<IndexTypeDTO>> getAllIndexTypes() {
        List<VIndexType> results = vIndexTypeRepository.findAllByOrderByIdAsc();
        List<IndexTypeDTO> dtos = results.stream()
            .map(this::convertIndexTypeToDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    private IndexTypeDTO convertIndexTypeToDTO(VIndexType indexType) {
        return new IndexTypeDTO(
            indexType.getId(),
            indexType.getDescripcion()
        );
    }
    
    // ========================================
    // EVENT TYPES ENDPOINTS
    // ========================================
    
    @GetMapping("/event-types")
    public ResponseEntity<List<TipoEventoDTO>> getAllEventTypes() {
        System.out.println("CatalogController: GET /api/catalogs/event-types");
        List<TipoEvento> results = tipoEventoRepository.findAllByOrderByIdAsc();
        
        System.out.println("CatalogController: Encontrados " + results.size() + " tipos de evento");
        
        List<TipoEventoDTO> dtos = results.stream()
            .map(this::convertEventTypeToDTO)
            .collect(Collectors.toList());
        
        System.out.println("CatalogController: Retornando " + dtos.size() + " tipos de evento");
        return ResponseEntity.ok(dtos);
    }
    
    @GetMapping("/event-types/{id}")
    public ResponseEntity<TipoEventoDTO> getEventTypeById(@PathVariable Long id) {
        System.out.println("CatalogController: GET /api/catalogs/event-types/" + id);
        return tipoEventoRepository.findById(id)
            .map(eventType -> {
                System.out.println("CatalogController: Tipo de evento encontrado: " + eventType.getDescripcion());
                return ResponseEntity.ok(convertEventTypeToDTO(eventType));
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    private TipoEventoDTO convertEventTypeToDTO(TipoEvento tipoEvento) {
        TipoEventoDTO dto = new TipoEventoDTO();
        dto.setId(tipoEvento.getId());
        dto.setIdDescripcion(tipoEvento.getIdDescripcion());
        dto.setDescripcion(tipoEvento.getDescripcion());
        dto.setCreatedAt(tipoEvento.getCreatedAt() != null ? tipoEvento.getCreatedAt().toString() : null);
        dto.setUpdatedAt(tipoEvento.getUpdatedAt() != null ? tipoEvento.getUpdatedAt().toString() : null);
        return dto;
    }
    
    // ========================================
    // COUNTRIES ENDPOINTS
    // ========================================
    
    @GetMapping("/countries")
    public ResponseEntity<List<PaisDTO>> getAllCountries() {
        System.out.println("CatalogController: GET /api/catalogs/countries");
        List<Pais> results = paisRepository.findAllByOrderByIdDescripcionAsc();
        
        System.out.println("CatalogController: Encontrados " + results.size() + " países");
        
        List<PaisDTO> dtos = results.stream()
            .map(this::convertCountryToDTO)
            .collect(Collectors.toList());
        
        System.out.println("CatalogController: Retornando " + dtos.size() + " países");
        return ResponseEntity.ok(dtos);
    }
    
    @GetMapping("/countries/{codigo}")
    public ResponseEntity<PaisDTO> getCountryByCode(@PathVariable String codigo) {
        System.out.println("CatalogController: GET /api/catalogs/countries/" + codigo);
        return paisRepository.findById(codigo)
            .map(country -> {
                System.out.println("CatalogController: País encontrado: " + country.getIdDescripcion());
                return ResponseEntity.ok(convertCountryToDTO(country));
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    private PaisDTO convertCountryToDTO(Pais pais) {
        PaisDTO dto = new PaisDTO();
        dto.setCodigo(pais.getCodigo());
        dto.setIdDescripcion(pais.getIdDescripcion());
        return dto;
    }
    
    // ========================================
    // INSTITUTIONS ENDPOINTS
    // ========================================
    
    @GetMapping("/institutions")
    public ResponseEntity<List<InstitucionDTO>> getAllInstitutions() {
        List<Institucion> results = institucionRepository.findAllByOrderByIdDescripcionAsc();
        
        List<InstitucionDTO> dtos = results.stream()
            .map(this::convertInstitutionToDTO)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }
    
    @GetMapping("/institutions/{id}")
    public ResponseEntity<InstitucionDTO> getInstitutionById(@PathVariable Long id) {
        return institucionRepository.findById(id)
            .map(institution -> ResponseEntity.ok(convertInstitutionToDTO(institution)))
            .orElse(ResponseEntity.notFound().build());
    }
    
    private InstitucionDTO convertInstitutionToDTO(Institucion institucion) {
        InstitucionDTO dto = new InstitucionDTO();
        dto.setId(institucion.getId());
        dto.setIdDescripcion(institucion.getIdDescripcion());
        dto.setDescripcion(institucion.getDescripcion());
        return dto;
    }
    
    // ========================================
    // ACADEMIC DEGREES ENDPOINTS
    // ========================================
    
    @GetMapping("/academic-degrees")
    public ResponseEntity<List<GradoAcademicoDTO>> getAllAcademicDegrees() {
        System.out.println("CatalogController: GET /api/catalogs/academic-degrees");
        List<GradoAcademico> results = gradoAcademicoRepository.findAllByOrderByIdAsc();
        
        System.out.println("CatalogController: Encontrados " + results.size() + " grados académicos");
        
        List<GradoAcademicoDTO> dtos = results.stream()
            .map(this::convertAcademicDegreeToDTO)
            .collect(Collectors.toList());
        
        System.out.println("CatalogController: Retornando " + dtos.size() + " grados académicos");
        return ResponseEntity.ok(dtos);
    }
    
    @GetMapping("/academic-degrees/{id}")
    public ResponseEntity<GradoAcademicoDTO> getAcademicDegreeById(@PathVariable Long id) {
        System.out.println("CatalogController: GET /api/catalogs/academic-degrees/" + id);
        return gradoAcademicoRepository.findById(id)
            .map(degree -> {
                System.out.println("CatalogController: Grado académico encontrado: " + degree.getDescripcion());
                return ResponseEntity.ok(convertAcademicDegreeToDTO(degree));
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    private GradoAcademicoDTO convertAcademicDegreeToDTO(GradoAcademico grado) {
        GradoAcademicoDTO dto = new GradoAcademicoDTO();
        dto.setId(grado.getId());
        dto.setIdDescripcion(grado.getIdDescripcion());
        dto.setDescripcion(grado.getDescripcion());
        return dto;
    }
    
    // ========================================
    // THESIS STATUS ENDPOINTS
    // ========================================
    
    @GetMapping("/thesis-status")
    public ResponseEntity<List<EstadoTesisDTO>> getAllThesisStatus() {
        System.out.println("CatalogController: GET /api/catalogs/thesis-status");
        List<EstadoTesis> results = estadoTesisRepository.findAllByOrderByIdAsc();
        
        System.out.println("CatalogController: Encontrados " + results.size() + " estados de tesis");
        
        List<EstadoTesisDTO> dtos = results.stream()
            .map(this::convertThesisStatusToDTO)
            .collect(Collectors.toList());
        
        System.out.println("CatalogController: Retornando " + dtos.size() + " estados de tesis");
        return ResponseEntity.ok(dtos);
    }
    
    @GetMapping("/thesis-status/{id}")
    public ResponseEntity<EstadoTesisDTO> getThesisStatusById(@PathVariable Long id) {
        System.out.println("CatalogController: GET /api/catalogs/thesis-status/" + id);
        return estadoTesisRepository.findById(id)
            .map(status -> {
                System.out.println("CatalogController: Estado de tesis encontrado: " + status.getDescripcion());
                return ResponseEntity.ok(convertThesisStatusToDTO(status));
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    private EstadoTesisDTO convertThesisStatusToDTO(EstadoTesis estado) {
        EstadoTesisDTO dto = new EstadoTesisDTO();
        dto.setId(estado.getId());
        dto.setIdDescripcion(estado.getIdDescripcion());
        dto.setDescripcion(estado.getDescripcion());
        return dto;
    }
    
    // ========================================
    // SECTOR TYPES ENDPOINTS
    // ========================================
    
    @GetMapping("/sector-types")
    public ResponseEntity<List<TipoSectorDTO>> getAllSectorTypes() {
        System.out.println("CatalogController: GET /api/catalogs/sector-types");
        List<TipoSector> results = tipoSectorRepository.findAllByOrderByIdAsc();
        
        System.out.println("CatalogController: Encontrados " + results.size() + " tipos de sector");
        
        List<TipoSectorDTO> dtos = results.stream()
            .map(this::convertSectorTypeToDTO)
            .collect(Collectors.toList());
        
        System.out.println("CatalogController: Retornando " + dtos.size() + " tipos de sector");
        return ResponseEntity.ok(dtos);
    }
    
    @GetMapping("/sector-types/{id}")
    public ResponseEntity<TipoSectorDTO> getSectorTypeById(@PathVariable Long id) {
        System.out.println("CatalogController: GET /api/catalogs/sector-types/" + id);
        return tipoSectorRepository.findById(id)
            .map(sector -> {
                System.out.println("CatalogController: Tipo de sector encontrado: " + sector.getIdDescripcion());
                return ResponseEntity.ok(convertSectorTypeToDTO(sector));
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    private TipoSectorDTO convertSectorTypeToDTO(TipoSector sector) {
        TipoSectorDTO dto = new TipoSectorDTO();
        dto.setId(sector.getId());
        dto.setIdDescripcion(sector.getIdDescripcion());
        return dto;
    }
    
    // ========================================
    // TRANSFER TYPES ENDPOINTS
    // ========================================
    
    @GetMapping("/transfer-types")
    public ResponseEntity<List<TipoTransferenciaDTO>> getAllTransferTypes() {
        System.out.println("CatalogController: GET /api/catalogs/transfer-types");
        List<TipoTransferencia> results = tipoTransferenciaRepository.findAllByOrderByIdAsc();
        
        System.out.println("CatalogController: Encontrados " + results.size() + " tipos de transferencia");
        
        List<TipoTransferenciaDTO> dtos = results.stream()
            .map(this::convertTransferTypeToDTO)
            .collect(Collectors.toList());
        
        System.out.println("CatalogController: Retornando " + dtos.size() + " tipos de transferencia");
        return ResponseEntity.ok(dtos);
    }
    
    @GetMapping("/transfer-types/{id}")
    public ResponseEntity<TipoTransferenciaDTO> getTransferTypeById(@PathVariable Long id) {
        System.out.println("CatalogController: GET /api/catalogs/transfer-types/" + id);
        return tipoTransferenciaRepository.findById(id)
            .map(transferType -> {
                System.out.println("CatalogController: Tipo de transferencia encontrado: " + transferType.getIdDescripcion());
                return ResponseEntity.ok(convertTransferTypeToDTO(transferType));
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    private TipoTransferenciaDTO convertTransferTypeToDTO(TipoTransferencia transferType) {
        TipoTransferenciaDTO dto = new TipoTransferenciaDTO();
        dto.setId(transferType.getId());
        dto.setIdDescripcion(transferType.getIdDescripcion());
        return dto;
    }
    
    // ========================================
    // TRANSFER CATEGORIES ENDPOINTS
    // ========================================
    
    @GetMapping("/transfer-categories")
    public ResponseEntity<List<CategoriaTransferenciaDTO>> getAllTransferCategories() {
        System.out.println("CatalogController: GET /api/catalogs/transfer-categories");
        List<CategoriaTransferencia> results = categoriaTransferenciaRepository.findAllByOrderByIdAsc();
        
        System.out.println("CatalogController: Encontradas " + results.size() + " categorías de transferencia");
        
        List<CategoriaTransferenciaDTO> dtos = results.stream()
            .map(this::convertTransferCategoryToDTO)
            .collect(Collectors.toList());
        
        System.out.println("CatalogController: Retornando " + dtos.size() + " categorías de transferencia");
        return ResponseEntity.ok(dtos);
    }
    
    @GetMapping("/transfer-categories/{id}")
    public ResponseEntity<CategoriaTransferenciaDTO> getTransferCategoryById(@PathVariable Long id) {
        System.out.println("CatalogController: GET /api/catalogs/transfer-categories/" + id);
        return categoriaTransferenciaRepository.findById(id)
            .map(category -> {
                System.out.println("CatalogController: Categoría de transferencia encontrada: " + category.getIdDescripcion());
                return ResponseEntity.ok(convertTransferCategoryToDTO(category));
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    private CategoriaTransferenciaDTO convertTransferCategoryToDTO(CategoriaTransferencia category) {
        CategoriaTransferenciaDTO dto = new CategoriaTransferenciaDTO();
        dto.setId(category.getId());
        dto.setIdDescripcion(category.getIdDescripcion());
        return dto;
    }
    
    // ========================================
    // RESOURCES ENDPOINTS
    // ========================================
    
    @GetMapping("/resources")
    public ResponseEntity<List<ResourceDTO>> getAllResources() {
        System.out.println("CatalogController: GET /api/catalogs/resources");
        List<Resource> results = resourceRepository.findAll();
        
        System.out.println("CatalogController: Encontrados " + results.size() + " recursos");
        
        List<ResourceDTO> dtos = results.stream()
            .map(this::convertResourceToDTO)
            .collect(Collectors.toList());
        
        System.out.println("CatalogController: Retornando " + dtos.size() + " recursos");
        return ResponseEntity.ok(dtos);
    }
    
    @GetMapping("/resources/{id}")
    public ResponseEntity<ResourceDTO> getResourceById(@PathVariable Long id) {
        System.out.println("CatalogController: GET /api/catalogs/resources/" + id);
        return resourceRepository.findById(id)
            .map(resource -> {
                System.out.println("CatalogController: Recurso encontrado: " + resource.getIdDescripcion());
                return ResponseEntity.ok(convertResourceToDTO(resource));
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    private ResourceDTO convertResourceToDTO(Resource resource) {
        ResourceDTO dto = new ResourceDTO();
        dto.setId(resource.getId());
        dto.setIdDescripcion(resource.getIdDescripcion());
        return dto;
    }
    
    // ========================================
    // FUNDING TYPES ENDPOINTS
    // ========================================
    
    @GetMapping("/funding-types")
    public ResponseEntity<List<FundingTypeDTO>> getAllFundingTypes() {
        System.out.println("CatalogController: GET /api/catalogs/funding-types");
        List<FundingType> results = fundingTypeRepository.findAll();
        
        System.out.println("CatalogController: Encontrados " + results.size() + " tipos de financiamiento");
        
        List<FundingTypeDTO> dtos = results.stream()
            .map(this::convertFundingTypeToDTO)
            .collect(Collectors.toList());
        
        System.out.println("CatalogController: Retornando " + dtos.size() + " tipos de financiamiento");
        return ResponseEntity.ok(dtos);
    }
    
    @GetMapping("/funding-types/{id}")
    public ResponseEntity<FundingTypeDTO> getFundingTypeById(@PathVariable Long id) {
        System.out.println("CatalogController: GET /api/catalogs/funding-types/" + id);
        return fundingTypeRepository.findById(id)
            .map(fundingType -> {
                System.out.println("CatalogController: Tipo de financiamiento encontrado: " + fundingType.getIdDescripcion());
                return ResponseEntity.ok(convertFundingTypeToDTO(fundingType));
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    private FundingTypeDTO convertFundingTypeToDTO(FundingType fundingType) {
        FundingTypeDTO dto = new FundingTypeDTO();
        dto.setId(fundingType.getId());
        dto.setIdDescripcion(fundingType.getIdDescripcion());
        return dto;
    }
    
    // ========================================
    // TIPO DIFUSION ENDPOINTS
    // ========================================
    
    @GetMapping("/diffusion-types")
    public ResponseEntity<List<TipoDifusionDTO>> getAllDiffusionTypes() {
        List<TipoDifusion> results = tipoDifusionRepository.findAllByOrderByIdAsc();
        List<TipoDifusionDTO> dtos = results.stream()
            .map(this::convertTipoDifusionToDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
    
    @GetMapping("/diffusion-types/{id}")
    public ResponseEntity<TipoDifusionDTO> getDiffusionTypeById(@PathVariable Long id) {
        return tipoDifusionRepository.findById(id)
            .map(diffusionType -> ResponseEntity.ok(convertTipoDifusionToDTO(diffusionType)))
            .orElse(ResponseEntity.notFound().build());
    }
    
    private TipoDifusionDTO convertTipoDifusionToDTO(TipoDifusion tipoDifusion) {
        TipoDifusionDTO dto = new TipoDifusionDTO();
        dto.setId(tipoDifusion.getId());
        dto.setIdDescripcion(tipoDifusion.getIdDescripcion());
        return dto;
    }
    
    // ========================================
    // PUBLICO OBJETIVO ENDPOINTS
    // ========================================
    
    @GetMapping("/target-audiences")
    public ResponseEntity<List<PublicoObjetivoDTO>> getAllTargetAudiences() {
        List<PublicoObjetivo> results = publicoObjetivoRepository.findAllByOrderByIdAsc();
        List<PublicoObjetivoDTO> dtos = results.stream()
            .map(this::convertPublicoObjetivoToDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
    
    @GetMapping("/target-audiences/{id}")
    public ResponseEntity<PublicoObjetivoDTO> getTargetAudienceById(@PathVariable Long id) {
        return publicoObjetivoRepository.findById(id)
            .map(targetAudience -> ResponseEntity.ok(convertPublicoObjetivoToDTO(targetAudience)))
            .orElse(ResponseEntity.notFound().build());
    }
    
    private PublicoObjetivoDTO convertPublicoObjetivoToDTO(PublicoObjetivo publicoObjetivo) {
        PublicoObjetivoDTO dto = new PublicoObjetivoDTO();
        dto.setId(publicoObjetivo.getId());
        dto.setIdDescripcion(publicoObjetivo.getIdDescripcion());
        return dto;
    }
    
    // ========================================
    // TIPO COLABORACION ENDPOINTS
    // ========================================
    
    @Autowired
    private TipoColaboracionRepository tipoColaboracionRepository;
    
    @Autowired
    private TipoRRHHRepository tipoRRHHRepository;
    
    @GetMapping("/collaboration-types")
    public ResponseEntity<List<TipoColaboracionDTO>> getAllCollaborationTypes() {
        List<TipoColaboracion> results = tipoColaboracionRepository.findAllByOrderByIdAsc();
        List<TipoColaboracionDTO> dtos = results.stream()
            .map(this::convertTipoColaboracionToDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
    
    @GetMapping("/collaboration-types/{id}")
    public ResponseEntity<TipoColaboracionDTO> getCollaborationTypeById(@PathVariable Long id) {
        return tipoColaboracionRepository.findById(id)
            .map(collaborationType -> ResponseEntity.ok(convertTipoColaboracionToDTO(collaborationType)))
            .orElse(ResponseEntity.notFound().build());
    }
    
    private TipoColaboracionDTO convertTipoColaboracionToDTO(TipoColaboracion tipoColaboracion) {
        TipoColaboracionDTO dto = new TipoColaboracionDTO();
        dto.setId(tipoColaboracion.getId());
        dto.setIdDescripcion(tipoColaboracion.getIdDescripcion());
        return dto;
    }
    
    // ========================================
    // PERIODS ENDPOINTS
    // ========================================
    
    @GetMapping("/periods")
    public ResponseEntity<List<Map<String, Object>>> getAllPeriods() {
        // Los períodos son valores calculados basados en fechas
        // 1: hasta 2022-07-31
        // 2: hasta 2023-07-31
        // 3: hasta 2024-07-31
        // 4: hasta 2025-07-31
        // 5: después de 2025-07-31
        List<Map<String, Object>> periods = new java.util.ArrayList<>();
        
        Map<String, Object> period1 = new HashMap<>();
        period1.put("id", 1);
        period1.put("name", "Período 1");
        period1.put("description", "Hasta 31 de julio 2022");
        periods.add(period1);
        
        Map<String, Object> period2 = new HashMap<>();
        period2.put("id", 2);
        period2.put("name", "Período 2");
        period2.put("description", "Hasta 31 de julio 2023");
        periods.add(period2);
        
        Map<String, Object> period3 = new HashMap<>();
        period3.put("id", 3);
        period3.put("name", "Período 3");
        period3.put("description", "Hasta 31 de julio 2024");
        periods.add(period3);
        
        Map<String, Object> period4 = new HashMap<>();
        period4.put("id", 4);
        period4.put("name", "Período 4");
        period4.put("description", "Hasta 31 de julio 2025");
        periods.add(period4);
        
        Map<String, Object> period5 = new HashMap<>();
        period5.put("id", 5);
        period5.put("name", "Período 5");
        period5.put("description", "Después de 31 de julio 2025");
        periods.add(period5);
        
        return ResponseEntity.ok(periods);
    }
    
    // ========================================
    // TIPO RRHH ENDPOINTS
    // ========================================
    
    @GetMapping("/rrhh-types")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<List<TipoRRHHDTO>> getAllRRHHTypes() {
        List<TipoRRHH> results = tipoRRHHRepository.findAll();
        List<TipoRRHHDTO> dtos = results.stream()
            .map(this::convertTipoRRHHToDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
    
    @GetMapping("/rrhh-types/{id}")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<TipoRRHHDTO> getRRHHTypeById(@PathVariable Long id) {
        return tipoRRHHRepository.findById(id)
            .map(tipoRRHH -> ResponseEntity.ok(convertTipoRRHHToDTO(tipoRRHH)))
            .orElse(ResponseEntity.notFound().build());
    }
    
    private TipoRRHHDTO convertTipoRRHHToDTO(TipoRRHH tipoRRHH) {
        if (tipoRRHH == null) return null;
        
        TipoRRHHDTO dto = new TipoRRHHDTO();
        dto.setId(tipoRRHH.getId());
        dto.setIdDescripcion(tipoRRHH.getCodigoDescripcion());
        dto.setDescripcion(tipoRRHH.getDescripcion());
        dto.setCreatedAt(tipoRRHH.getCreatedAt() != null ? tipoRRHH.getCreatedAt().toString() : null);
        dto.setUpdatedAt(tipoRRHH.getUpdatedAt() != null ? tipoRRHH.getUpdatedAt().toString() : null);
        return dto;
    }
}


