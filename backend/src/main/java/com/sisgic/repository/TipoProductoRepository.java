package com.sisgic.repository;

import com.sisgic.entity.TipoProducto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TipoProductoRepository extends JpaRepository<TipoProducto, Long> {
    
    // Buscar todos ordenados por ID
    List<TipoProducto> findAllByOrderByIdAsc();
    
    // Query personalizada para obtener descripciones con JOIN a textos
    @Query("SELECT tp.id, tp.idDescripcion, t.valor as descripcion " +
           "FROM TipoProducto tp " +
           "LEFT JOIN Textos t ON tp.idDescripcion = t.id.codigoTexto " +
           "WHERE t.id.idTipoTexto = 2 AND t.id.lenguaje = 'us' " +
           "ORDER BY tp.id ASC")
    List<Object[]> findAllWithDescription();
}
