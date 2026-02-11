package com.sisgic.repository;

import com.sisgic.entity.TipoEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TipoEventoRepository extends JpaRepository<TipoEvento, Long> {
    List<TipoEvento> findAllByOrderByIdAsc();
}










