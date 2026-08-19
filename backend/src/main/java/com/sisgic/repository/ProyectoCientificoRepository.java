package com.sisgic.repository;

import com.sisgic.entity.ProyectoCientifico;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProyectoCientificoRepository extends JpaRepository<ProyectoCientifico, Long> {

    boolean existsByProjectCodeIgnoreCase(String projectCode);

    boolean existsByProjectCodeIgnoreCaseAndIdNot(String projectCode, Long id);

    @Query(value = "SELECT p.id, p.projectCode, p.awardDate, p.duration, p.totalAmount, p.totalAmountCenter, " +
           "p.idFundingtype, p.otherFundingType, p.projectTypes, p.otherProjectType, " +
           "p.nameSocialOrganizations, p.namePublicSectorEntities, p.namePrivateSectorEntities, " +
           "p.nameTradeRegionalAssociations, p.nameSTEntities, " +
           "pr.idDescripcion, pr.idComentario, pr.fechaInicio, pr.fechaTermino, " +
           "pr.idTipoProducto, pr.urlDocumento, pr.linkVisualizacion, pr.linkPDF, pr.progressReport, " +
           "pr.idEstadoProducto, pr.codigoANID, pr.basal, pr.nameResearchLine, pr.cluster, pr.created_at, pr.updated_at, " +
           "pr.username, " +
           "f_getRRHHProducto(p.id) as participantesNombres, " +
           "f_getParticipantByRol(p.id, 20) as mainResponsible " +
           "FROM proyecto p " +
           "INNER JOIN producto pr ON p.id = pr.id " +
           "WHERE f_productIsVisible(p.id, pr.username, :idRRHH, :userName) = 1",
           countQuery = "SELECT COUNT(*) FROM proyecto p INNER JOIN producto pr ON p.id = pr.id " +
               "WHERE f_productIsVisible(p.id, pr.username, :idRRHH, :userName) = 1",
           nativeQuery = true)
    Page<ProyectoCientifico> findVisibleByUserIdRRHH(
        @Param("idRRHH") Long idRRHH,
        @Param("userName") String userName,
        Pageable pageable);

    @Query(value = "SELECT p.id, p.projectCode, p.awardDate, p.duration, p.totalAmount, p.totalAmountCenter, " +
           "p.idFundingtype, p.otherFundingType, p.projectTypes, p.otherProjectType, " +
           "p.nameSocialOrganizations, p.namePublicSectorEntities, p.namePrivateSectorEntities, " +
           "p.nameTradeRegionalAssociations, p.nameSTEntities, " +
           "pr.idDescripcion, pr.idComentario, pr.fechaInicio, pr.fechaTermino, " +
           "pr.idTipoProducto, pr.urlDocumento, pr.linkVisualizacion, pr.linkPDF, pr.progressReport, " +
           "pr.idEstadoProducto, pr.codigoANID, pr.basal, pr.nameResearchLine, pr.cluster, pr.created_at, pr.updated_at, " +
           "pr.username, " +
           "f_getRRHHProducto(p.id) as participantesNombres, " +
           "f_getParticipantByRol(p.id, 20) as mainResponsible " +
           "FROM proyecto p " +
           "INNER JOIN producto pr ON p.id = pr.id " +
           "WHERE p.id = :id AND f_productIsVisible(p.id, pr.username, :idRRHH, :userName) = 1",
           nativeQuery = true)
    Optional<ProyectoCientifico> findVisibleByIdAndUserIdRRHH(
        @Param("id") Long id,
        @Param("idRRHH") Long idRRHH,
        @Param("userName") String userName);
}
