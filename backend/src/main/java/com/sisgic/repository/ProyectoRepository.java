package com.sisgic.repository;

import com.sisgic.entity.Proyecto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProyectoRepository extends JpaRepository<Proyecto, String> {
    /**
     * Busca un proyecto por su código
     */
    java.util.Optional<Proyecto> findByCodigo(String codigo);
}





