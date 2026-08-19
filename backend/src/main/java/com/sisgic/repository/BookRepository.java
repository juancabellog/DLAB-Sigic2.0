package com.sisgic.repository;

import com.sisgic.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    @Query(value = "SELECT b.id, b.idBookType, b.chapterTitle, b.firstPage, b.lastPage, " +
           "b.editorialCityCountry, b.year, b.ISBN, " +
           "pr.idDescripcion, pr.idComentario, pr.fechaInicio, pr.fechaTermino, " +
           "pr.idTipoProducto, pr.urlDocumento, pr.linkVisualizacion, pr.linkPDF, pr.progressReport, " +
           "pr.idEstadoProducto, pr.codigoANID, pr.basal, pr.nameResearchLine, pr.cluster, pr.created_at, pr.updated_at, " +
           "pr.username, " +
           "f_getRRHHProducto(b.id) as participantesNombres " +
           "FROM book b " +
           "INNER JOIN producto pr ON b.id = pr.id " +
           "WHERE f_productIsVisible(b.id, pr.username, :idRRHH, :userName) = 1",
           countQuery = "SELECT COUNT(*) FROM book b INNER JOIN producto pr ON b.id = pr.id " +
               "WHERE f_productIsVisible(b.id, pr.username, :idRRHH, :userName) = 1",
           nativeQuery = true)
    Page<Book> findVisibleByUserIdRRHH(
        @Param("idRRHH") Long idRRHH,
        @Param("userName") String userName,
        Pageable pageable);

    @Query(value = "SELECT b.id, b.idBookType, b.chapterTitle, b.firstPage, b.lastPage, " +
           "b.editorialCityCountry, b.year, b.ISBN, " +
           "pr.idDescripcion, pr.idComentario, pr.fechaInicio, pr.fechaTermino, " +
           "pr.idTipoProducto, pr.urlDocumento, pr.linkVisualizacion, pr.linkPDF, pr.progressReport, " +
           "pr.idEstadoProducto, pr.codigoANID, pr.basal, pr.nameResearchLine, pr.cluster, pr.created_at, pr.updated_at, " +
           "pr.username, " +
           "f_getRRHHProducto(b.id) as participantesNombres " +
           "FROM book b " +
           "INNER JOIN producto pr ON b.id = pr.id " +
           "WHERE b.id = :id AND f_productIsVisible(b.id, pr.username, :idRRHH, :userName) = 1",
           nativeQuery = true)
    Optional<Book> findVisibleByIdAndUserIdRRHH(
        @Param("id") Long id,
        @Param("idRRHH") Long idRRHH,
        @Param("userName") String userName);
}
