package com.sisgic.controller.admin;

import com.sisgic.service.CacheEvictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controlador REST para administración de cachés.
 * 
 * Este controlador expone endpoints para invalidar y recargar cachés cuando
 * la aplicación antigua de administración modifica las tablas de catálogo o textos.
 * 
 * Los endpoints están diseñados para ser llamados por la aplicación antigua
 * después de realizar modificaciones en las tablas correspondientes.
 * 
 * Ejemplo de uso desde la aplicación antigua:
 * - Después de modificar un catálogo: POST /sigic2.0/api/admin/cache/catalogos/evict
 * - Después de modificar textos: POST /sigic2.0/api/admin/cache/textos/evict
 * - Para invalidar todo: POST /sigic2.0/api/admin/cache/evictAll
 * 
 * Nota: Estos endpoints deberían estar protegidos en producción, pero por ahora
 * están accesibles para facilitar la integración con la aplicación antigua.
 */
@RestController
@RequestMapping("/api/admin/cache")
@CrossOrigin(origins = "*")
public class AdminCacheController {
    
    @Autowired
    private CacheEvictionService cacheEvictionService;
    
    /**
     * Invalida el caché de catálogos.
     * 
     * Debe ser llamado por la aplicación antigua después de modificar cualquier
     * tabla de catálogo (journals, tipos de participación, tipos de producto, etc.)
     * 
     * @return Respuesta con mensaje de confirmación
     */
    @PostMapping("/catalogos/evict")
    public ResponseEntity<Map<String, String>> evictCatalogosCache() {
        cacheEvictionService.evictCatalogosCache();
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Caché de catálogos invalidado correctamente");
        response.put("cache", "catalogos");
        return ResponseEntity.ok(response);
    }
    
    /**
     * Invalida el caché de textos.
     * 
     * Debe ser llamado por la aplicación antigua después de modificar la tabla
     * de textos (textos).
     * 
     * @return Respuesta con mensaje de confirmación
     */
    @PostMapping("/textos/evict")
    public ResponseEntity<Map<String, String>> evictTextosCache() {
        cacheEvictionService.evictTextosCache();
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Caché de textos invalidado correctamente");
        response.put("cache", "textos");
        return ResponseEntity.ok(response);
    }
    
    /**
     * Invalida todos los cachés.
     * 
     * Debe ser llamado por la aplicación antigua cuando se realizan cambios
     * masivos en catálogos o textos.
     * 
     * @return Respuesta con mensaje de confirmación
     */
    @PostMapping("/evictAll")
    public ResponseEntity<Map<String, String>> evictAllCaches() {
        cacheEvictionService.evictAllCaches();
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Todos los cachés invalidados correctamente");
        response.put("caches", "catalogos,textos");
        return ResponseEntity.ok(response);
    }
    
    /**
     * Obtiene información sobre el estado de los cachés.
     * 
     * @return Respuesta con información de los cachés
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getCacheStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "active");
        response.put("caches", new String[]{"catalogos", "textos"});
        response.put("message", "Los cachés están activos. Use los endpoints /evict para invalidarlos.");
        return ResponseEntity.ok(response);
    }
}
