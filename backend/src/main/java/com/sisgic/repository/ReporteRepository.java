package com.sisgic.repository;

import com.sisgic.entity.Reporte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReporteRepository extends JpaRepository<Reporte, Long> {
    
    @Query("SELECT r.excelFile FROM Reporte r WHERE r.id = :id")
    Optional<byte[]> findExcelFileById(@Param("id") Long id);
}
