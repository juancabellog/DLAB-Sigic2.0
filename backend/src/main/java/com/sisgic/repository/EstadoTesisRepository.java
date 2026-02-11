package com.sisgic.repository;

import com.sisgic.entity.EstadoTesis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EstadoTesisRepository extends JpaRepository<EstadoTesis, Long> {
    List<EstadoTesis> findAllByOrderByIdAsc();
}










