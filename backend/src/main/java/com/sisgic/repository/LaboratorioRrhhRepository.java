package com.sisgic.repository;

import com.sisgic.entity.LaboratorioRrhh;
import com.sisgic.entity.LaboratorioRrhhId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LaboratorioRrhhRepository extends JpaRepository<LaboratorioRrhh, LaboratorioRrhhId> {

    @Query("""
        SELECT lr FROM LaboratorioRrhh lr
        JOIN FETCH lr.rrhh r
        LEFT JOIN FETCH r.tipoRRHH
        WHERE lr.codigoCentro = :codigoCentro
          AND lr.idArea = :idArea
          AND lr.idLaboratorio = :idLaboratorio
        ORDER BY lr.fechaInicio DESC
        """)
    List<LaboratorioRrhh> findByLaboratorio(
        @Param("codigoCentro") String codigoCentro,
        @Param("idArea") Long idArea,
        @Param("idLaboratorio") Long idLaboratorio
    );

    @Query("""
        SELECT lr FROM LaboratorioRrhh lr
        JOIN FETCH lr.rrhh r
        LEFT JOIN FETCH r.tipoRRHH
        WHERE lr.codigoCentro = :codigoCentro
          AND lr.idArea = :idArea
          AND lr.idLaboratorio = :idLaboratorio
          AND lr.tipoRecurso = :tipoRecurso
        ORDER BY lr.fechaInicio DESC
        """)
    List<LaboratorioRrhh> findByLaboratorioAndTipo(
        @Param("codigoCentro") String codigoCentro,
        @Param("idArea") Long idArea,
        @Param("idLaboratorio") Long idLaboratorio,
        @Param("tipoRecurso") String tipoRecurso
    );

    @Query("""
        SELECT COUNT(lr) FROM LaboratorioRrhh lr
        WHERE lr.codigoCentro = :codigoCentro
          AND lr.idArea = :idArea
          AND lr.idLaboratorio = :idLaboratorio
          AND lr.tipoRecurso = 'I'
          AND lr.fechaTermino IS NULL
        """)
    long countActiveMembers(
        @Param("codigoCentro") String codigoCentro,
        @Param("idArea") Long idArea,
        @Param("idLaboratorio") Long idLaboratorio
    );

    @Query("""
        SELECT CASE WHEN COUNT(lr) > 0 THEN true ELSE false END FROM LaboratorioRrhh lr
        WHERE lr.codigoCentro = :codigoCentro
          AND lr.idArea = :idArea
          AND lr.idLaboratorio = :idLaboratorio
          AND lr.tipoRecurso = :tipoRecurso
          AND lr.fechaTermino IS NULL
        """)
    boolean hasActiveByTipo(
        @Param("codigoCentro") String codigoCentro,
        @Param("idArea") Long idArea,
        @Param("idLaboratorio") Long idLaboratorio,
        @Param("tipoRecurso") String tipoRecurso
    );

    long countByCodigoCentroAndIdAreaAndIdLaboratorio(String codigoCentro, Long idArea, Long idLaboratorio);

    void deleteByCodigoCentroAndIdAreaAndIdLaboratorio(String codigoCentro, Long idArea, Long idLaboratorio);
}
