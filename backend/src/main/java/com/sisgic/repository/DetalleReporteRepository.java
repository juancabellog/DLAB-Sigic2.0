package com.sisgic.repository;

import com.sisgic.entity.DetalleReporte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DetalleReporteRepository extends JpaRepository<DetalleReporte, Long> {
    
    List<DetalleReporte> findByIdReporteOrderByIdAsc(Long idReporte);
    
    @Query("SELECT d FROM DetalleReporte d WHERE d.idReporte = :idReporte ORDER BY d.id ASC")
    List<DetalleReporte> findByReporteId(@Param("idReporte") Long idReporte);
}
