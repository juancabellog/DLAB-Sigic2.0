package com.sisgic.repository;

import com.sisgic.entity.TipoProyecto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TipoProyectoRepository extends JpaRepository<TipoProyecto, Long> {
    List<TipoProyecto> findAllByOrderByIdAsc();
    Optional<TipoProyecto> findByIdDescripcionIgnoreCase(String idDescripcion);
}
