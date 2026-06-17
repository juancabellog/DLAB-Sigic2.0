package com.sisgic.repository;

import com.sisgic.entity.EstadoNoticia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EstadoNoticiaRepository extends JpaRepository<EstadoNoticia, Long> {
}
