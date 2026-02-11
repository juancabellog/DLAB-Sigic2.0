package com.sisgic.repository;

import com.sisgic.entity.ParticipacionProducto;
import com.sisgic.entity.ParticipacionProductoId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParticipacionProductoRepository extends JpaRepository<ParticipacionProducto, ParticipacionProductoId> {
    
    /**
     * Busca todas las participaciones de un producto
     */
    @Query("SELECT pp FROM ParticipacionProducto pp " +
           "LEFT JOIN FETCH pp.rrhh " +
           "LEFT JOIN FETCH pp.tipoParticipacion " +
           "WHERE pp.producto.id = :productoId " +
           "ORDER BY pp.orden ASC")
    List<ParticipacionProducto> findByProductoId(@Param("productoId") Long productoId);
    
    /**
     * Elimina todas las participaciones de un producto
     */
    @Modifying
    @Query("DELETE FROM ParticipacionProducto pp WHERE pp.producto.id = :productoId")
    void deleteByProductoId(@Param("productoId") Long productoId);
    
    /**
     * Obtiene el siguiente ID correlativo para una combinación de (idProducto, idRRHH)
     * El id es un correlativo que se incrementa para cada combinación única
     */
    @Query(value = "SELECT COALESCE(MAX(id), 0) + 1 FROM rrhh_producto " +
                   "WHERE idProducto = :productoId AND idRRHH = :rrhhId",
           nativeQuery = true)
    Long getNextIdForParticipacion(@Param("productoId") Long productoId, 
                                    @Param("rrhhId") Long rrhhId);
}


