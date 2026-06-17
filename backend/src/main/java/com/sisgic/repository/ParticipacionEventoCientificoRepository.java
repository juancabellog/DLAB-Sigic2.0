package com.sisgic.repository;

import com.sisgic.entity.ParticipacionEventoCientifico;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipacionEventoCientificoRepository extends JpaRepository<ParticipacionEventoCientifico, Long> {

    List<ParticipacionEventoCientifico> findAllByOrderByIdAsc();

    @Query("SELECT DISTINCT p FROM ParticipacionEventoCientifico p " +
            "LEFT JOIN FETCH p.tipoParticipacionEvento " +
            "LEFT JOIN FETCH p.pais " +
            "LEFT JOIN FETCH p.tipoProducto " +
            "LEFT JOIN FETCH p.estadoProducto " +
            "WHERE p.id = :id")
    Optional<ParticipacionEventoCientifico> findByIdWithRelations(@Param("id") Long id);

    @Query(value = "SELECT p.id, p.idModalidadPresentacion, p.idTipoParticipacionEvento, p.codigoPais, p.ciudad, p.nameResearchLine, p.eventName, " +
            "pr.idDescripcion, pr.idComentario, pr.fechaInicio, pr.fechaTermino, pr.idTipoProducto, pr.urlDocumento, pr.linkVisualizacion, pr.linkPDF, " +
            "pr.progressReport, pr.idEstadoProducto, pr.codigoANID, pr.basal, pr.nameResearchLine as productoNameResearchLine, pr.cluster, pr.created_at, pr.updated_at, pr.username, " +
            "f_getRRHHProducto(p.id) as participantesNombres " +
            "FROM participacioneventocientifico p " +
            "INNER JOIN producto pr ON p.id = pr.id " +
            "WHERE f_productIsVisible(p.id, pr.username, :idRRHH, :userName) = 1",
            countQuery = "SELECT COUNT(*) FROM participacioneventocientifico p INNER JOIN producto pr ON p.id = pr.id WHERE f_productIsVisible(p.id, pr.username, :idRRHH, :userName) = 1",
            nativeQuery = true)
    Page<ParticipacionEventoCientifico> findVisibleByUserIdRRHH(@Param("idRRHH") Long idRRHH, @Param("userName") String userName, Pageable pageable);

    @Query(value = "SELECT p.id, p.idModalidadPresentacion, p.idTipoParticipacionEvento, p.codigoPais, p.ciudad, p.nameResearchLine, p.eventName, " +
            "pr.idDescripcion, pr.idComentario, pr.fechaInicio, pr.fechaTermino, pr.idTipoProducto, pr.urlDocumento, pr.linkVisualizacion, pr.linkPDF, " +
            "pr.progressReport, pr.idEstadoProducto, pr.codigoANID, pr.basal, pr.nameResearchLine as productoNameResearchLine, pr.cluster, pr.created_at, pr.updated_at, pr.username, " +
            "f_getRRHHProducto(p.id) as participantesNombres " +
            "FROM participacioneventocientifico p " +
            "INNER JOIN producto pr ON p.id = pr.id " +
            "WHERE p.id = :id AND f_productIsVisible(p.id, pr.username, :idRRHH, :userName) = 1",
            nativeQuery = true)
    Optional<ParticipacionEventoCientifico> findVisibleByIdAndUserIdRRHH(@Param("id") Long id, @Param("idRRHH") Long idRRHH, @Param("userName") String userName);
}
