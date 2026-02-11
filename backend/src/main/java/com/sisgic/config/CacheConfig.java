package com.sisgic.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Configuración de caché para la aplicación.
 * 
 * Esta configuración utiliza Caffeine como proveedor de caché para optimizar
 * el acceso a datos de catálogos y textos que son modificados por la aplicación
 * antigua de administración.
 * 
 * Los cachés se configuran con TTL (Time To Live) y tamaño máximo para evitar
 * consumo excesivo de memoria.
 */
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Value("${cache.catalogos.max-size:1000}")
    private int catalogosMaxSize;
    
    @Value("${cache.catalogos.ttl-minutes:30}")
    private int catalogosTtlMinutes;
    
    @Value("${cache.textos.max-size:5000}")
    private int textosMaxSize;
    
    @Value("${cache.textos.ttl-minutes:60}")
    private int textosTtlMinutes;
    
    /**
     * Configura el CacheManager usando Caffeine.
     * 
     * Define dos cachés principales:
     * - "catalogos": Para datos de catálogos (journals, tipos de participación, etc.)
     * - "textos": Para textos/traducciones por código y lenguaje
     * 
     * @return CacheManager configurado con Caffeine
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("catalogos", "textos");
        cacheManager.setCaffeine(caffeineCacheBuilder());
        return cacheManager;
    }
    
    /**
     * Configura el builder de Caffeine con parámetros por defecto.
     * Los cachés específicos pueden sobrescribir estos valores.
     */
    private Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .recordStats();
    }
    
    /**
     * Bean para el caché de catálogos.
     * Configurado con tamaño y TTL específicos para datos de catálogo.
     */
    @Bean
    public com.github.benmanes.caffeine.cache.Cache<Object, Object> catalogosCache() {
        return Caffeine.newBuilder()
                .maximumSize(catalogosMaxSize)
                .expireAfterWrite(catalogosTtlMinutes, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }
    
    /**
     * Bean para el caché de textos.
     * Configurado con tamaño mayor y TTL más largo ya que los textos cambian menos frecuentemente.
     */
    @Bean
    public com.github.benmanes.caffeine.cache.Cache<Object, Object> textosCache() {
        return Caffeine.newBuilder()
                .maximumSize(textosMaxSize)
                .expireAfterWrite(textosTtlMinutes, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }
}

