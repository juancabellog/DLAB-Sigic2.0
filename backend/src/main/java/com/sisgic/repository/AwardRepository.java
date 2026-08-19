package com.sisgic.repository;

import com.sisgic.entity.Award;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AwardRepository extends JpaRepository<Award, Long> {

    @Query(value = "SELECT a.id, a.year, a.idinstitucion, " +
           "pr.idDescripcion, pr.idComentario, pr.fechaInicio, pr.fechaTermino, " +
           "pr.idTipoProducto, pr.urlDocumento, pr.linkVisualizacion, pr.linkPDF, pr.progressReport, " +
           "pr.idEstadoProducto, pr.codigoANID, pr.basal, pr.nameResearchLine, pr.cluster, pr.created_at, pr.updated_at, " +
           "pr.username, " +
           "f_getRRHHProducto(a.id) as participantesNombres " +
           "FROM award a " +
           "INNER JOIN producto pr ON a.id = pr.id " +
           "WHERE f_productIsVisible(a.id, pr.username, :idRRHH, :userName) = 1",
           countQuery = "SELECT COUNT(*) FROM award a INNER JOIN producto pr ON a.id = pr.id " +
               "WHERE f_productIsVisible(a.id, pr.username, :idRRHH, :userName) = 1",
           nativeQuery = true)
    Page<Award> findVisibleByUserIdRRHH(
        @Param("idRRHH") Long idRRHH,
        @Param("userName") String userName,
        Pageable pageable);

    @Query(value = "SELECT a.id, a.year, a.idinstitucion, " +
           "pr.idDescripcion, pr.idComentario, pr.fechaInicio, pr.fechaTermino, " +
           "pr.idTipoProducto, pr.urlDocumento, pr.linkVisualizacion, pr.linkPDF, pr.progressReport, " +
           "pr.idEstadoProducto, pr.codigoANID, pr.basal, pr.nameResearchLine, pr.cluster, pr.created_at, pr.updated_at, " +
           "pr.username, " +
           "f_getRRHHProducto(a.id) as participantesNombres " +
           "FROM award a " +
           "INNER JOIN producto pr ON a.id = pr.id " +
           "WHERE a.id = :id AND f_productIsVisible(a.id, pr.username, :idRRHH, :userName) = 1",
           nativeQuery = true)
    Optional<Award> findVisibleByIdAndUserIdRRHH(
        @Param("id") Long id,
        @Param("idRRHH") Long idRRHH,
        @Param("userName") String userName);
}
