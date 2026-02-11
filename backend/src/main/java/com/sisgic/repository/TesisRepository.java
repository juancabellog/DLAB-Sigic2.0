package com.sisgic.repository;

import com.sisgic.entity.Tesis;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TesisRepository extends JpaRepository<Tesis, Long> {
    
    List<Tesis> findAllByOrderByIdAsc();
    
    @Query("SELECT DISTINCT t FROM Tesis t " +
           "LEFT JOIN FETCH t.institucionOG " +
           "LEFT JOIN FETCH t.gradoAcademico " +
           "LEFT JOIN FETCH t.institucion " +
           "LEFT JOIN FETCH t.estadoTesis " +
           "LEFT JOIN FETCH t.tipoProducto " +
           "LEFT JOIN FETCH t.estadoProducto " +
           "LEFT JOIN FETCH t.participantes " +
           "WHERE t.id = :id")
    Optional<Tesis> findByIdWithRelations(@Param("id") Long id);
    
    @Query("SELECT DISTINCT t FROM Tesis t " +
           "LEFT JOIN FETCH t.institucionOG " +
           "LEFT JOIN FETCH t.gradoAcademico " +
           "LEFT JOIN FETCH t.institucion " +
           "LEFT JOIN FETCH t.estadoTesis " +
           "LEFT JOIN FETCH t.tipoProducto " +
           "LEFT JOIN FETCH t.estadoProducto " +
           "LEFT JOIN FETCH t.participantes")
    List<Tesis> findAllWithRelations();
    
    /**
     * Obtiene tesis visibles para un usuario específico usando f_productIsVisible
     * Hace JOIN con producto para obtener todas las columnas (herencia JOINED)
     * Incluye participantesNombres usando la función MySQL f_getRRHHProducto
     * @param idRRHH ID del recurso humano del usuario (puede ser null)
     * @param pageable Paginación
     * @return Página de tesis visibles
     */
    @Query(value = "SELECT t.id, t.idInstitucionOG, t.idGradoAcademico, t.idInstitucion, t.idEstadoTesis, " +
           "t.fechaInicioPrograma, t.nombreCompletoTitulo, t.tipoSector, " +
           "pr.idDescripcion, pr.idComentario, pr.fechaInicio, pr.fechaTermino, " +
           "pr.idTipoProducto, pr.urlDocumento, pr.linkVisualizacion, pr.linkPDF, pr.progressReport, " +
           "pr.idEstadoProducto, pr.codigoANID, pr.basal, pr.nameResearchLine, pr.created_at, pr.updated_at, " +
           "pr.username, " +
           "f_getRRHHProducto(t.id) as participantesNombres, " +
           "f_getParticipantByRol(t.id, 7) as estudiante " +
           "FROM tesis t " +
           "INNER JOIN producto pr ON t.id = pr.id " +
           "WHERE f_productIsVisible(t.id, pr.username, :idRRHH, :userName) = 1",
           countQuery = "SELECT COUNT(*) FROM tesis t INNER JOIN producto pr ON t.id = pr.id WHERE f_productIsVisible(t.id, pr.username, :idRRHH, :userName) = 1",
           nativeQuery = true)
    Page<Tesis> findVisibleByUserIdRRHH(@Param("idRRHH") Long idRRHH, @Param("userName") String userName, Pageable pageable);
    
    /**
     * Obtiene una tesis visible para un usuario específico usando f_productIsVisible
     * Hace JOIN con producto para obtener todas las columnas (herencia JOINED)
     * Incluye participantesNombres usando la función MySQL f_getRRHHProducto
     * @param id ID de la tesis
     * @param idRRHH ID del recurso humano del usuario
     * @return Optional con la tesis si es visible, o vacío si no lo es
     */
    @Query(value = "SELECT t.id, t.idInstitucionOG, t.idGradoAcademico, t.idInstitucion, t.idEstadoTesis, " +
           "t.fechaInicioPrograma, t.nombreCompletoTitulo, t.tipoSector, " +
           "pr.idDescripcion, pr.idComentario, pr.fechaInicio, pr.fechaTermino, " +
           "pr.idTipoProducto, pr.urlDocumento, pr.linkVisualizacion, pr.linkPDF, pr.progressReport, " +
           "pr.idEstadoProducto, pr.codigoANID, pr.basal, pr.nameResearchLine, pr.created_at, pr.updated_at, " +
           "pr.username, " +
           "f_getRRHHProducto(t.id) as participantesNombres, " +
           "f_getParticipantByRol(t.id, 7) as estudiante " +
           "FROM tesis t " +
           "INNER JOIN producto pr ON t.id = pr.id " +
           "WHERE t.id = :id AND f_productIsVisible(t.id, pr.username, :idRRHH, :userName) = 1",
           nativeQuery = true)
    Optional<Tesis> findVisibleByIdAndUserIdRRHH(@Param("id") Long id, @Param("idRRHH") Long idRRHH, @Param("userName") String userName);
}










