package com.sisgic.repository;

import com.sisgic.entity.PublicoObjetivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PublicoObjetivoRepository extends JpaRepository<PublicoObjetivo, Long> {
    List<PublicoObjetivo> findAllByOrderByIdAsc();
}










