package com.sisgic.repository;

import com.sisgic.entity.Pais;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaisRepository extends JpaRepository<Pais, String> {
    List<Pais> findAllByOrderByIdDescripcionAsc();
}










