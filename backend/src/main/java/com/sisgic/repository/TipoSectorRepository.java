package com.sisgic.repository;

import com.sisgic.entity.TipoSector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TipoSectorRepository extends JpaRepository<TipoSector, Long> {
    List<TipoSector> findAllByOrderByIdAsc();
}










