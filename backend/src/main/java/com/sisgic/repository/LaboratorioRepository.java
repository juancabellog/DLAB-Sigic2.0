package com.sisgic.repository;

import com.sisgic.entity.Laboratorio;
import com.sisgic.entity.LaboratorioId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LaboratorioRepository extends JpaRepository<Laboratorio, LaboratorioId> {

    @Query("SELECT COALESCE(MAX(l.id), 0) FROM Laboratorio l WHERE l.codigoCentro = :codigoCentro")
    Long findMaxIdByCodigoCentro(@Param("codigoCentro") String codigoCentro);

    List<Laboratorio> findByCodigoCentroAndId(String codigoCentro, Long id);

    Optional<Laboratorio> findByCodigoCentroAndIdAndIdArea(String codigoCentro, Long id, Long idArea);

    @Query("""
        SELECT l FROM Laboratorio l
        WHERE l.codigoCentro = :codigoCentro
          AND (:activo IS NULL OR l.activo = :activo)
          AND (:idArea IS NULL OR l.idArea = :idArea)
        """)
    List<Laboratorio> findFiltered(
        @Param("codigoCentro") String codigoCentro,
        @Param("activo") String activo,
        @Param("idArea") Long idArea
    );
}
