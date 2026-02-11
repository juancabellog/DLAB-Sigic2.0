package com.sisgic.repository;

import com.sisgic.entity.Institucion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InstitucionRepository extends JpaRepository<Institucion, Long> {
    List<Institucion> findAllByOrderByIdDescripcionAsc();
}










