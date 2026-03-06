package com.sisgic.repository;

import com.sisgic.entity.Difusion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DifusionRepository extends JpaRepository<Difusion, Long> {
    
    @Query("SELECT d FROM Difusion d " +
           "LEFT JOIN FETCH d.tipoDifusion " +
           "LEFT JOIN FETCH d.pais " +
           "LEFT JOIN FETCH d.tipoProducto " +
           "LEFT JOIN FETCH d.estadoProducto")
    Page<Difusion> findAllWithRelations(Pageable pageable);
    
    @Query("SELECT d FROM Difusion d " +
           "LEFT JOIN FETCH d.tipoDifusion " +
           "LEFT JOIN FETCH d.pais " +
           "LEFT JOIN FETCH d.tipoProducto " +
           "LEFT JOIN FETCH d.estadoProducto " +
           "WHERE d.id = :id")
    java.util.Optional<Difusion> findByIdWithRelations(@Param("id") Long id);
    
    /**
     * Obtiene difusiones visibles para un usuario específico usando f_productIsVisible
     * Hace JOIN con producto para obtener todas las columnas (herencia JOINED)
     * Incluye participantesNombres usando la función MySQL f_getRRHHProducto
     * @param idRRHH ID del recurso humano del usuario (puede ser null)
     * @param pageable Paginación
     * @return Página de difusiones visibles
     */
    @Query(value = "SELECT d.id, d.idTipoDifusion, d.codigoPais, d.lugar, d.numAsistentes, " +
           "d.duracion, d.publicoObjetivo, d.ciudad, d.link, " +
           "pr.idDescripcion, pr.idComentario, pr.fechaInicio, pr.fechaTermino, " +
           "pr.idTipoProducto, pr.urlDocumento, pr.linkVisualizacion, pr.linkPDF, pr.progressReport, " +
           "pr.idEstadoProducto, pr.codigoANID, pr.basal, pr.nameResearchLine, pr.cluster, pr.created_at, pr.updated_at, " +
           "pr.username, " +
           "f_getRRHHProducto(d.id) as participantesNombres, " +
           "f_getParticipantByRol(d.id, 20) as mainResponsible " +
           "FROM difusion d " +
           "INNER JOIN producto pr ON d.id = pr.id " +
           "WHERE f_productIsVisible(d.id, pr.username, :idRRHH, :userName) = 1",
           countQuery = "SELECT COUNT(*) FROM difusion d INNER JOIN producto pr ON d.id = pr.id WHERE f_productIsVisible(d.id, pr.username, :idRRHH, :userName) = 1",
           nativeQuery = true)
    Page<Difusion> findVisibleByUserIdRRHH(@Param("idRRHH") Long idRRHH, @Param("userName") String userName, Pageable pageable);
    
    /**
     * Obtiene una difusión visible para un usuario específico usando f_productIsVisible
     * Hace JOIN con producto para obtener todas las columnas (herencia JOINED)
     * Incluye participantesNombres usando la función MySQL f_getRRHHProducto
     * @param id ID de la difusión
     * @param idRRHH ID del recurso humano del usuario
     * @return Optional con la difusión si es visible, o vacío si no lo es
     */
    @Query(value = "SELECT d.id, d.idTipoDifusion, d.codigoPais, d.lugar, d.numAsistentes, " +
           "d.duracion, d.publicoObjetivo, d.ciudad, d.link, " +
           "pr.idDescripcion, pr.idComentario, pr.fechaInicio, pr.fechaTermino, " +
           "pr.idTipoProducto, pr.urlDocumento, pr.linkVisualizacion, pr.linkPDF, pr.progressReport, " +
           "pr.idEstadoProducto, pr.codigoANID, pr.basal, pr.nameResearchLine, pr.cluster, pr.created_at, pr.updated_at, " +
           "pr.username, " +
           "f_getRRHHProducto(d.id) as participantesNombres, " +
           "f_getParticipantByRol(d.id, 20) as mainResponsible " +
           "FROM difusion d " +
           "INNER JOIN producto pr ON d.id = pr.id " +
           "WHERE d.id = :id AND f_productIsVisible(d.id, pr.username, :idRRHH, :userName) = 1",
           nativeQuery = true)
    java.util.Optional<Difusion> findVisibleByIdAndUserIdRRHH(@Param("id") Long id, @Param("idRRHH") Long idRRHH, @Param("userName") String userName);
}

