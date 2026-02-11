package com.sisgic.repository;

import com.sisgic.entity.TipoTransferencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TipoTransferenciaRepository extends JpaRepository<TipoTransferencia, Long> {
    List<TipoTransferencia> findAllByOrderByIdAsc();
}










