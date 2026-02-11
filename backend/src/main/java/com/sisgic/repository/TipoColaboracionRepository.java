package com.sisgic.repository;

import com.sisgic.entity.TipoColaboracion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TipoColaboracionRepository extends JpaRepository<TipoColaboracion, Long> {
    List<TipoColaboracion> findAllByOrderByIdAsc();
}










