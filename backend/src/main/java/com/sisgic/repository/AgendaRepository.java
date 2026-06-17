package com.sisgic.repository;

import com.sisgic.entity.Agenda;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface AgendaRepository extends JpaRepository<Agenda, Long> {

    @Query("SELECT DISTINCT a FROM Agenda a " +
           "LEFT JOIN FETCH a.estado " +
           "WHERE (:estadoId IS NULL OR a.estado.id = :estadoId) " +
           "AND (:fromDate IS NULL OR a.fechaInicio >= :fromDate) " +
           "AND (:toDate IS NULL OR a.fechaInicio <= :toDate) " +
           "AND (:location IS NULL OR :location = '' OR LOWER(a.lugar) LIKE LOWER(CONCAT('%', :location, '%'))) " +
           "AND (:title IS NULL OR :title = '' OR EXISTS (" +
           "  SELECT 1 FROM Textos t WHERE t.id.codigoTexto = a.descripcion " +
           "  AND t.id.idTipoTexto = 2 " +
           "  AND LOWER(t.valor) LIKE LOWER(CONCAT('%', :title, '%'))))")
    Page<Agenda> findByFilters(
        @Param("estadoId") Long estadoId,
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate,
        @Param("location") String location,
        @Param("title") String title,
        Pageable pageable);

    @Query("SELECT a FROM Agenda a LEFT JOIN FETCH a.estado WHERE a.id = :id")
    Optional<Agenda> findByIdWithRelations(@Param("id") Long id);
}
