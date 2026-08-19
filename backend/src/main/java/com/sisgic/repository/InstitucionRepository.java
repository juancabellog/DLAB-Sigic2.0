package com.sisgic.repository;

import com.sisgic.entity.Institucion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface InstitucionRepository extends JpaRepository<Institucion, Long> {
    List<Institucion> findAllByOrderByIdDescripcionAsc();

    @Query(value = "SELECT COALESCE(MAX(id), 0) + 1 FROM institucion", nativeQuery = true)
    Long getNextId();

    /**
     * Case-insensitive exact match on the translated institution name from the view.
     */
    @Query(value = "SELECT i.id, i.idDescripcion, i.descripcion, i.codigoPais " +
           "FROM v_institucion i " +
           "WHERE LOWER(TRIM(i.descripcion)) = LOWER(TRIM(:name)) " +
           "LIMIT 1",
           nativeQuery = true)
    Optional<Institucion> findByDescripcionIgnoreCase(@Param("name") String name);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO institucion (id, idDescripcion, codigoPais) " +
           "VALUES (:id, :idDescripcion, :codigoPais)",
           nativeQuery = true)
    int insertInstitution(
        @Param("id") Long id,
        @Param("idDescripcion") String idDescripcion,
        @Param("codigoPais") String codigoPais);
}
