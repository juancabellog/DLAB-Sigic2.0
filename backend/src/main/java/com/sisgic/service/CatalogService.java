package com.sisgic.service;

import com.sisgic.dto.JournalDTO;
import com.sisgic.repository.JournalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Servicio para acceso a datos de catálogos con caché.
 * 
 * Este servicio encapsula el acceso a catálogos y aplica caché para optimizar
 * el rendimiento. Los datos se cachean en memoria y pueden ser invalidados
 * mediante los endpoints de AdminCacheController cuando la aplicación antigua
 * modifica las tablas de catálogo.
 */
@Service
@Transactional(readOnly = true)
public class CatalogService {
    
    @Autowired
    private JournalRepository journalRepository;
    
    /**
     * Obtiene todos los journals con descripción resuelta desde textos.
     * El resultado se cachea en el caché "catalogos".
     * 
     * @param language Idioma para la descripción (por defecto "us")
     * @return Lista de JournalDTO con descripción resuelta
     */
    @Cacheable(value = "catalogos", key = "'journals_' + #language")
    public List<JournalDTO> getAllJournals(String language) {
        return journalRepository.findAllByLanguage(language);
    }
    
    /**
     * Obtiene un journal por ID con descripción resuelta desde textos.
     * El resultado se cachea en el caché "catalogos".
     * 
     * @param id ID del journal
     * @param language Idioma para la descripción (por defecto "us")
     * @return Optional con JournalDTO si existe
     */
    @Cacheable(value = "catalogos", key = "'journal_' + #id + '_' + #language")
    public Optional<JournalDTO> getJournalById(Long id, String language) {
        return journalRepository.findByIdWithDescription(id, language);
    }
}

