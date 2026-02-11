package com.sisgic.repository;

import com.sisgic.entity.TipoParticipacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TipoParticipacionRepository extends JpaRepository<TipoParticipacion, Long> {
    
    // Buscar por tipo de producto
    @Query("SELECT tp FROM TipoParticipacion tp WHERE tp.idTipoProducto = :idTipoProducto ORDER BY tp.id ASC")
    List<TipoParticipacion> findByIdTipoProductoOrderByIdAsc(@Param("idTipoProducto") Long idTipoProducto);
    
    // Buscar por tipo de producto usando query nativa (alternativa por si hay problemas de tipo)
    @Query(value = "SELECT * FROM v_tipo_participacion WHERE idTipoProducto = :idTipoProducto ORDER BY id ASC", nativeQuery = true)
    List<TipoParticipacion> findByIdTipoProductoOrderByIdAscNative(@Param("idTipoProducto") Long idTipoProducto);
    
    // Contar por tipo de producto
    @Query("SELECT COUNT(tp) FROM TipoParticipacion tp WHERE tp.idTipoProducto = :idTipoProducto")
    long countByIdTipoProducto(@Param("idTipoProducto") Long idTipoProducto);
    
    // Contar por tipo de producto usando query nativa
    @Query(value = "SELECT COUNT(*) FROM v_tipo_participacion WHERE idTipoProducto = :idTipoProducto", nativeQuery = true)
    long countByIdTipoProductoNative(@Param("idTipoProducto") Long idTipoProducto);
    
    // Buscar todos ordenados por ID
    List<TipoParticipacion> findAllByOrderByIdAsc();
    
    // Query personalizada para obtener descripciones con JOIN a textos
    @Query("SELECT tp.id, tp.idDescripcion, tp.idTipoProducto, t.valor as descripcion " +
           "FROM TipoParticipacion tp " +
           "LEFT JOIN Textos t ON tp.idDescripcion = t.id.codigoTexto " +
           "WHERE t.id.idTipoTexto = 2 AND t.id.lenguaje = 'us' " +
           "ORDER BY tp.id ASC")
    List<Object[]> findAllWithDescription();
    
    // Query para obtener por tipo de producto con descripción
    @Query("SELECT tp.id, tp.idDescripcion, tp.idTipoProducto, t.valor as descripcion " +
           "FROM TipoParticipacion tp " +
           "LEFT JOIN Textos t ON tp.idDescripcion = t.id.codigoTexto " +
           "WHERE tp.idTipoProducto = :idTipoProducto " +
           "AND t.id.idTipoTexto = 2 AND t.id.lenguaje = 'us' " +
           "ORDER BY tp.id ASC")
    List<Object[]> findByIdTipoProductoWithDescription(@Param("idTipoProducto") Long idTipoProducto);
}



