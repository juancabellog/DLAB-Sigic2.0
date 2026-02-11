package com.sisgic.service;

import com.sisgic.entity.Textos;
import com.sisgic.entity.TextosId;
import com.sisgic.repository.TextosRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class TextosService {
    
    @Autowired
    private TextosRepository textosRepository;
    
    /**
     * Genera un código único para un nuevo texto
     */
    public String generateCodigoTexto(Integer idTipoTexto) {
        Optional<Long> maxCodigo = textosRepository.findMaxCodigoTexto(idTipoTexto);
        Long nextNumber = maxCodigo.orElse(0L) + 1;
        return String.format("TXT%06d", nextNumber);
    }
    
    /**
     * Crea un texto en ambos idiomas (us y es)
     * Invalida el caché de textos después de crear
     */
    @CacheEvict(value = "textos", allEntries = true)
    public String createTextInBothLanguages(String valor, Integer idTipoTexto) {
        String codigoTexto = generateCodigoTexto(idTipoTexto);
        
        // Crear registro para inglés (us)
        TextosId idUs = new TextosId("us", codigoTexto, idTipoTexto);
        Textos textoUs = new Textos(idUs, valor);
        textosRepository.save(textoUs);
        
        // Crear registro para español (es)
        TextosId idEs = new TextosId("es", codigoTexto, idTipoTexto);
        Textos textoEs = new Textos(idEs, valor);
        textosRepository.save(textoEs);
        
        return codigoTexto;
    }
    
    /**
     * Actualiza un texto en ambos idiomas
     * Invalida el caché de textos después de actualizar
     */
    @CacheEvict(value = "textos", allEntries = true)
    public void updateTextInBothLanguages(String codigoTexto, String valor, Integer idTipoTexto) {
        // Actualizar inglés (us)
        TextosId idUs = new TextosId("us", codigoTexto, idTipoTexto);
        Optional<Textos> textoUsOpt = textosRepository.findById(idUs);
        if (textoUsOpt.isPresent()) {
            Textos textoUs = textoUsOpt.get();
            textoUs.setValor(valor);
            textosRepository.save(textoUs);
        }
        
        // Actualizar español (es)
        TextosId idEs = new TextosId("es", codigoTexto, idTipoTexto);
        Optional<Textos> textoEsOpt = textosRepository.findById(idEs);
        if (textoEsOpt.isPresent()) {
            Textos textoEs = textoEsOpt.get();
            textoEs.setValor(valor);
            textosRepository.save(textoEs);
        }
    }
    
    /**
     * Elimina un texto de ambos idiomas
     * Invalida el caché de textos después de eliminar
     */
    @CacheEvict(value = "textos", allEntries = true)
    public void deleteTextFromBothLanguages(String codigoTexto, Integer idTipoTexto) {
        // Eliminar inglés (us)
        TextosId idUs = new TextosId("us", codigoTexto, idTipoTexto);
        textosRepository.deleteById(idUs);
        
        // Eliminar español (es)
        TextosId idEs = new TextosId("es", codigoTexto, idTipoTexto);
        textosRepository.deleteById(idEs);
    }
    
    /**
     * Obtiene el valor de un texto en un idioma específico.
     * El resultado se cachea en el caché "textos" con clave compuesta por
     * código de texto, tipo de texto y lenguaje.
     * 
     * @param codigoTexto Código del texto
     * @param idTipoTexto Tipo de texto (generalmente 2 para descripciones/comentarios)
     * @param lenguaje Idioma (us o es)
     * @return Optional con el valor del texto si existe
     */
    @Cacheable(value = "textos", key = "#codigoTexto + '_' + #idTipoTexto + '_' + #lenguaje")
    @Transactional(readOnly = true)
    public Optional<String> getTextValue(String codigoTexto, Integer idTipoTexto, String lenguaje) {
        Optional<Textos> texto = textosRepository.findByCodigoTextoAndTipoTextoAndLenguaje(codigoTexto, idTipoTexto, lenguaje);
        return texto.map(Textos::getValor);
    }
    
    /**
     * Carga múltiples textos de una vez y devuelve un Map para acceso rápido.
     * Esto optimiza las consultas cuando se cargan listas de productos.
     * El resultado se cachea en el caché "textos".
     * 
     * @param codigosTexto Lista de códigos de texto a cargar
     * @param idTipoTexto Tipo de texto (generalmente 2 para descripciones/comentarios)
     * @param lenguaje Idioma (us o es)
     * @return Map donde la clave es el código de texto y el valor es el texto traducido
     */
    @Cacheable(value = "textos", key = "'batch_' + T(String).join(',', #codigosTexto) + '_' + #idTipoTexto + '_' + #lenguaje")
    @Transactional(readOnly = true)
    public Map<String, String> getTextValuesBatch(List<String> codigosTexto, Integer idTipoTexto, String lenguaje) {
        if (codigosTexto == null || codigosTexto.isEmpty()) {
            return Collections.emptyMap();
        }
        
        // Filtrar códigos no nulos y no vacíos
        List<String> codigosValidos = codigosTexto.stream()
            .filter(codigo -> codigo != null && !codigo.isEmpty())
            .distinct()
            .collect(Collectors.toList());
        
        if (codigosValidos.isEmpty()) {
            return Collections.emptyMap();
        }
        
        // Cargar todos los textos de una vez
        List<Textos> textos = textosRepository.findByCodigosTextoAndTipoTextoAndLenguaje(codigosValidos, idTipoTexto, lenguaje);
        
        // Convertir a Map para acceso rápido
        return textos.stream()
            .collect(Collectors.toMap(
                t -> t.getId().getCodigoTexto(),
                Textos::getValor,
                (v1, v2) -> v1 // En caso de duplicados, tomar el primero
            ));
    }
}
