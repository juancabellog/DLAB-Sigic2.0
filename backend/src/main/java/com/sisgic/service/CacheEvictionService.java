package com.sisgic.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

/**
 * Servicio para invalidar cachés.
 * 
 * Este servicio encapsula la lógica de invalidación de cachés para que
 * Spring Cache pueda interceptar correctamente las anotaciones @CacheEvict.
 */
@Service
public class CacheEvictionService {
    
    /**
     * Invalida el caché de catálogos.
     * Este método debe ser llamado desde otro bean para que Spring Cache
     * pueda interceptar la anotación @CacheEvict.
     */
    @CacheEvict(value = "catalogos", allEntries = true)
    public void evictCatalogosCache() {
        // La invalidación se realiza mediante la anotación @CacheEvict
        // No necesita implementación adicional
    }
    
    /**
     * Invalida el caché de textos.
     * Este método debe ser llamado desde otro bean para que Spring Cache
     * pueda interceptar la anotación @CacheEvict.
     */
    @CacheEvict(value = "textos", allEntries = true)
    public void evictTextosCache() {
        // La invalidación se realiza mediante la anotación @CacheEvict
        // No necesita implementación adicional
    }
    
    /**
     * Invalida todos los cachés.
     * Este método debe ser llamado desde otro bean para que Spring Cache
     * pueda interceptar la anotación @CacheEvict.
     */
    @CacheEvict(value = {"catalogos", "textos"}, allEntries = true)
    public void evictAllCaches() {
        // La invalidación se realiza mediante la anotación @CacheEvict
        // No necesita implementación adicional
    }
}

