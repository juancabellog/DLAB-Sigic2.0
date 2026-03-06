package com.sisgic.repository;

import com.sisgic.entity.OrganizacionEventosCientificos;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizacionEventosCientificosRepository extends JpaRepository<OrganizacionEventosCientificos, Long> {
    
    List<OrganizacionEventosCientificos> findAllByOrderByIdAsc();
    
    @Query("SELECT DISTINCT o FROM OrganizacionEventosCientificos o " +
           "LEFT JOIN FETCH o.tipoEvento " +
           "LEFT JOIN FETCH o.pais " +
           "LEFT JOIN FETCH o.tipoProducto " +
           "LEFT JOIN FETCH o.estadoProducto " +
           "LEFT JOIN FETCH o.participantes " +
           "WHERE o.id = :id")
    Optional<OrganizacionEventosCientificos> findByIdWithRelations(@Param("id") Long id);
    
    @Query("SELECT DISTINCT o FROM OrganizacionEventosCientificos o " +
           "LEFT JOIN FETCH o.tipoEvento " +
           "LEFT JOIN FETCH o.pais " +
           "LEFT JOIN FETCH o.tipoProducto " +
           "LEFT JOIN FETCH o.estadoProducto " +
           "LEFT JOIN FETCH o.participantes")
    List<OrganizacionEventosCientificos> findAllWithRelations();
    
    /**
     * Obtiene eventos científicos visibles para un usuario específico usando f_productIsVisible
     * Hace JOIN con producto para obtener todas las columnas (herencia JOINED)
     * Incluye participantesNombres usando la función MySQL f_getRRHHProducto
     * @param idRRHH ID del recurso humano del usuario (puede ser null)
     * @param pageable Paginación
     * @return Página de eventos científicos visibles
     */
    @Query(value = "SELECT o.id, o.idTipoEvento, o.codigoPais, o.ciudad, o.numParticipantes, " +
           "pr.idDescripcion, pr.idComentario, pr.fechaInicio, pr.fechaTermino, " +
           "pr.idTipoProducto, pr.urlDocumento, pr.linkVisualizacion, pr.linkPDF, pr.progressReport, " +
           "pr.idEstadoProducto, pr.codigoANID, pr.basal, pr.nameResearchLine, pr.cluster, pr.created_at, pr.updated_at, " +
           "pr.username, " +
           "f_getRRHHProducto(o.id) as participantesNombres " +
           "FROM organizacioneventoscientificos o " +
           "INNER JOIN producto pr ON o.id = pr.id " +
           "WHERE f_productIsVisible(o.id, pr.username, :idRRHH, :userName) = 1",
           countQuery = "SELECT COUNT(*) FROM organizacioneventoscientificos o INNER JOIN producto pr ON o.id = pr.id WHERE f_productIsVisible(o.id, pr.username, :idRRHH, :userName) = 1",
           nativeQuery = true)
    Page<OrganizacionEventosCientificos> findVisibleByUserIdRRHH(@Param("idRRHH") Long idRRHH, @Param("userName") String userName, Pageable pageable);
    
    /**
     * Obtiene un evento científico visible para un usuario específico usando f_productIsVisible
     * Hace JOIN con producto para obtener todas las columnas (herencia JOINED)
     * Incluye participantesNombres usando la función MySQL f_getRRHHProducto
     * @param id ID del evento científico
     * @param idRRHH ID del recurso humano del usuario
     * @return Optional con el evento científico si es visible, o vacío si no lo es
     */
    @Query(value = "SELECT o.id, o.idTipoEvento, o.codigoPais, o.ciudad, o.numParticipantes, " +
           "pr.idDescripcion, pr.idComentario, pr.fechaInicio, pr.fechaTermino, " +
           "pr.idTipoProducto, pr.urlDocumento, pr.linkVisualizacion, pr.linkPDF, pr.progressReport, " +
           "pr.idEstadoProducto, pr.codigoANID, pr.basal, pr.nameResearchLine, pr.cluster, pr.created_at, pr.updated_at, " +
           "pr.username, " +
           "f_getRRHHProducto(o.id) as participantesNombres " +
           "FROM organizacioneventoscientificos o " +
           "INNER JOIN producto pr ON o.id = pr.id " +
           "WHERE o.id = :id AND f_productIsVisible(o.id, pr.username, :idRRHH, :userName) = 1",
           nativeQuery = true)
    Optional<OrganizacionEventosCientificos> findVisibleByIdAndUserIdRRHH(@Param("id") Long id, @Param("idRRHH") Long idRRHH, @Param("userName") String userName);
}

