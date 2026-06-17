package com.sisgic.repository;

import com.sisgic.entity.Noticia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface NoticiaRepository extends JpaRepository<Noticia, Long> {

    @Query("SELECT DISTINCT n FROM Noticia n " +
           "LEFT JOIN FETCH n.estado " +
           "LEFT JOIN n.tags tag " +
           "LEFT JOIN n.categories cat " +
           "WHERE (:estadoId IS NULL OR n.estado.id = :estadoId) " +
           "AND (:tagId IS NULL OR tag.id = :tagId) " +
           "AND (:categoryId IS NULL OR cat.id = :categoryId) " +
           "AND (:fromDate IS NULL OR n.fechaInicio >= :fromDate) " +
           "AND (:toDate IS NULL OR n.fechaTermino <= :toDate) " +
           "AND (:title IS NULL OR :title = '' OR EXISTS (" +
           "  SELECT 1 FROM Textos t WHERE t.id.codigoTexto = n.idTitulo " +
           "  AND t.id.idTipoTexto = 2 " +
           "  AND LOWER(t.valor) LIKE LOWER(CONCAT('%', :title, '%'))))")
    Page<Noticia> findByFilters(
        @Param("estadoId") Long estadoId,
        @Param("tagId") String tagId,
        @Param("categoryId") String categoryId,
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate,
        @Param("title") String title,
        Pageable pageable);

    @Query("SELECT DISTINCT n FROM Noticia n " +
           "LEFT JOIN FETCH n.estado " +
           "LEFT JOIN FETCH n.tags " +
           "LEFT JOIN FETCH n.categories " +
           "WHERE n.id = :id")
    Optional<Noticia> findByIdWithRelations(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Noticia n SET n.numVisitas = COALESCE(n.numVisitas, 0) + 1 WHERE n.id = :id")
    int incrementVisitas(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Noticia n SET n.numLikes = COALESCE(n.numLikes, 0) + 1 WHERE n.id = :id")
    int incrementLikes(@Param("id") Long id);
}
