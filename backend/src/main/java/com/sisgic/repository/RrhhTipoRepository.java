package com.sisgic.repository;

import com.sisgic.entity.RrhhTipo;
import com.sisgic.entity.RrhhTipoId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RrhhTipoRepository extends JpaRepository<RrhhTipo, RrhhTipoId> {

    @Query("SELECT r FROM RrhhTipo r WHERE r.idRRHH = :idRRHH AND r.fechaTermino IS NULL")
    List<RrhhTipo> findActiveByIdRRHH(@Param("idRRHH") Long idRRHH);

    List<RrhhTipo> findByIdRRHHOrderByFechaInicioDesc(Long idRRHH);
}
