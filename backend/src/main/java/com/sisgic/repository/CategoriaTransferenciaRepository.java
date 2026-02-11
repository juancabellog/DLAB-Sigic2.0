package com.sisgic.repository;

import com.sisgic.entity.CategoriaTransferencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoriaTransferenciaRepository extends JpaRepository<CategoriaTransferencia, Long> {
    List<CategoriaTransferencia> findAllByOrderByIdAsc();
}










