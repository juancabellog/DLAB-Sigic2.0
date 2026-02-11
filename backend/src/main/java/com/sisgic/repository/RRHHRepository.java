package com.sisgic.repository;

import com.sisgic.entity.RRHH;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Repository
public interface RRHHRepository extends JpaRepository<RRHH, Long> {
    
    @Query("SELECT r FROM RRHH r WHERE " +
           "(:name IS NULL OR LOWER(r.fullname) LIKE LOWER(CONCAT('%', :name, '%'))) " +
           "AND (:rut IS NULL OR r.idRecurso LIKE CONCAT('%', :rut, '%')) " +
           "AND (:orcid IS NULL OR r.orcid LIKE CONCAT('%', :orcid, '%'))")
    List<RRHH> search(@Param("name") String name, @Param("rut") String rut, @Param("orcid") String orcid);
    
    @Query("SELECT r FROM RRHH r LEFT JOIN FETCH r.tipoRRHH WHERE " +
           "LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(r.fullname, 'á', 'a'), 'é', 'e'), 'í', 'i'), 'ó', 'o'), 'ú', 'u'), 'ñ', 'n'), 'Á', 'A'), 'É', 'E'), 'Í', 'I'), 'Ó', 'O'), 'Ú', 'U'), 'Ñ', 'N'), 'ü', 'u'), 'Ü', 'U')) " +
           "LIKE LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(CONCAT('%', :query, '%'), 'á', 'a'), 'é', 'e'), 'í', 'i'), 'ó', 'o'), 'ú', 'u'), 'ñ', 'n'), 'Á', 'A'), 'É', 'E'), 'Í', 'I'), 'Ó', 'O'), 'Ú', 'U'), 'Ñ', 'N'), 'ü', 'u'), 'Ü', 'U')) " +
           "OR r.idRecurso LIKE CONCAT('%', :query, '%') " +
           "OR r.orcid LIKE CONCAT('%', :query, '%')")
    List<RRHH> findByQuery(@Param("query") String query);
    
    @Query("SELECT r FROM RRHH r LEFT JOIN FETCH r.tipoRRHH")
    Page<RRHH> findAllWithTipoRRHH(Pageable pageable);
    
    @Query("SELECT r FROM RRHH r LEFT JOIN FETCH r.tipoRRHH WHERE r.id = :id")
    RRHH findByIdWithTipoRRHH(@Param("id") Long id);
}