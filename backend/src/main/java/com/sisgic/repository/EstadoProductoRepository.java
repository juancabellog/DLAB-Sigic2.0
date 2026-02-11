package com.sisgic.repository;

import com.sisgic.entity.EstadoProducto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EstadoProductoRepository extends JpaRepository<EstadoProducto, Long> {
    
    /**
     * Obtiene todos los estados ordenados por ID
     */
    List<EstadoProducto> findAllByOrderByIdAsc();
}










