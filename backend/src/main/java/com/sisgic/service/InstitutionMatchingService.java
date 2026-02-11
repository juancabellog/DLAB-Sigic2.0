package com.sisgic.service;

import com.sisgic.entity.Institucion;
import com.sisgic.repository.InstitucionRepository;
import com.sisgic.service.TextosService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio para hacer matching de instituciones desde OpenAlex con nuestra base de datos
 * Carga todas las instituciones en memoria para búsquedas rápidas
 */
@Service
public class InstitutionMatchingService {
    
    @Autowired
    private InstitucionRepository institucionRepository;
    
    @Autowired
    private TextosService textosService;
    
    // Cache en memoria de todas las instituciones indexadas por nombre normalizado
    private HashMap<String, Long> hsInstitutions;
    
    /**
     * Carga todas las instituciones en memoria al inicializar el servicio
     */
    @PostConstruct
    public void loadInstitutions() {
        hsInstitutions = new HashMap<>();
        
        try {
            // Cargar todas las instituciones con sus textos en inglés
            List<Institucion> instituciones = institucionRepository.findAll();
            
            // Cargar todos los textos de instituciones en batch para evitar consultas individuales
            
            for (Institucion institucion : instituciones) {
                if (institucion.getIdDescripcion() != null) {
                    // Obtener el texto desde el mapa (ya cargado en batch y cacheado)
                    String valor = institucion.getIdDescripcion();
                    String normalizedName = ResearcherMatchingService.normalize(valor);
                    hsInstitutions.put(normalizedName, institucion.getId());
                }
            }
            
            System.out.println("InstitutionMatchingService: Cargadas " + hsInstitutions.size() + " instituciones en memoria");
        } catch (Exception e) {
            System.err.println("Error cargando instituciones en InstitutionMatchingService: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Busca una institución por nombre normalizado
     * 
     * @param name Nombre de la institución (será normalizado)
     * @param countryCode Código de país (opcional, para casos especiales)
     * @return ID de la institución si se encuentra, null si no
     */
    public Long getInstitution(String name, String countryCode) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        
        // Casos especiales hardcodeados (del código original)
        Long specialCase = getSpecialCaseInstitution(name);
        if (specialCase != null) {
            return specialCase;
        }
        
        // Buscar por nombre normalizado exacto
        String normalizedName = ResearcherMatchingService.normalize(name);
        Long exactMatch = hsInstitutions.get(normalizedName);
        if (exactMatch != null) {
            return exactMatch;
        }
        
        // Si no hay match exacto, intentar búsqueda parcial (para casos como "Finis Terrae University" vs "Universidad Finis Terrae")
        // Buscar por tokens clave en el nombre normalizado
        String[] tokens = normalizedName.split("\\s+");
        if (tokens.length >= 2) {
            // Buscar instituciones que contengan al menos 2 tokens importantes
            for (Map.Entry<String, Long> entry : hsInstitutions.entrySet()) {
                String dbNormalizedName = entry.getKey();
                int matchingTokens = 0;
                for (String token : tokens) {
                    // Solo considerar tokens de más de 3 caracteres para evitar matches falsos
                    if (token.length() > 3 && dbNormalizedName.contains(token)) {
                        matchingTokens++;
                    }
                }
                // Si al menos 2 tokens importantes coinciden, considerar match
                if (matchingTokens >= 2) {
                    return entry.getValue();
                }
            }
        }
        
        return null;
    }
    
    /**
     * Casos especiales de instituciones con nombres que no matchean bien
     */
    private Long getSpecialCaseInstitution(String name) {
        // Normalizar el nombre para comparación case-insensitive
        String normalized = name != null ? name.trim() : "";
        
        // Mapeo de casos especiales del código original
        if (normalized.equals("Metropolitan University of Technology")) {
            return 249L;
        }
        if (normalized.equals("University of Valparaíso")) {
            return 98L;
        }
        if (normalized.equals("University of Chile")) {
            return 2L;
        }
        if (normalized.equals("San Sebastián University")) {
            return 30L;
        }
        if (normalized.equals("Austral University of Chile")) {
            return 162L;
        }
        if (normalized.equals("Fundación Ciencia and Vida")) {
            return 99L;
        }
        if (normalized.equals("Pontificia Universidad Católica de Chile")) {
            return 10L;
        }
        if (normalized.equals("University of Talca")) {
            return 154L;
        }
        if (normalized.equals("Viña del Mar University")) {
            return 261L;
        }
        if (normalized.equals("University of Concepción")) {
            return 220L;
        }
        if (normalized.equals("Pontificia Universidad Católica de Valparaíso")) {
            return 114L;
        }
        if (normalized.equals("Universidad Bernardo O'Higgins")) {
            return 19L;
        }
        if (normalized.equals("Hospital Barros Luco-Trudeau")) {
            return 206L;
        }
        if (normalized.equals("Agriaquaculture Nutritional Genomic Center")) {
            return 254L;
        }
        if (normalized.equals("Hospital Clínico de la Universidad de Chile")) {
            return 138L;
        }
        if (normalized.equals("Universidad de Santiago de Chile")) {
            return 131L;
        }
        if (normalized.equals("Universidad Nacional Autónoma de México")) {
            return 250L;
        }
        if (normalized.equals("Universidad de Los Andes, Chile")) {
            return 128L;
        }
        if (normalized.equals("Millennium Nucleus of Ion Channel Associated Diseases")) {
            return 116L;
        }
        if (normalized.equals("Instituto de Física de Buenos Aires")) {
            return 10L;
        }
        if (normalized.equals("University of O'Higgins")) {
            return 19L;
        }
        
        return null;
    }
    
    /**
     * Recarga las instituciones en memoria
     */
    public void reloadInstitutions() {
        loadInstitutions();
    }
}

