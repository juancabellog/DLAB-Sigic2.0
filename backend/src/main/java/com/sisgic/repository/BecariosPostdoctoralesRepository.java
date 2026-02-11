package com.sisgic.repository;

import com.sisgic.entity.BecariosPostdoctorales;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BecariosPostdoctoralesRepository extends JpaRepository<BecariosPostdoctorales, Long> {
    
    List<BecariosPostdoctorales> findAllByOrderByIdAsc();
    
    @Query("SELECT DISTINCT bp FROM BecariosPostdoctorales bp " +
           "LEFT JOIN FETCH bp.institucion " +
           "LEFT JOIN FETCH bp.tipoSector " +
           "LEFT JOIN FETCH bp.tipoProducto " +
           "LEFT JOIN FETCH bp.estadoProducto " +
           "LEFT JOIN FETCH bp.participantes " +
           "WHERE bp.id = :id")
    Optional<BecariosPostdoctorales> findByIdWithRelations(@Param("id") Long id);
    
    @Query("SELECT DISTINCT bp FROM BecariosPostdoctorales bp " +
           "LEFT JOIN FETCH bp.institucion " +
           "LEFT JOIN FETCH bp.tipoSector " +
           "LEFT JOIN FETCH bp.tipoProducto " +
           "LEFT JOIN FETCH bp.estadoProducto " +
           "LEFT JOIN FETCH bp.participantes")
    List<BecariosPostdoctorales> findAllWithRelations();
    
    /**
     * Obtiene becarios postdoctorales visibles para un usuario específico usando f_productIsVisible
     * Hace JOIN con producto para obtener todas las columnas (herencia JOINED)
     * Incluye participantesNombres usando la función MySQL f_getRRHHProducto
     * @param idRRHH ID del recurso humano del usuario (puede ser null)
     * @param pageable Paginación
     * @return Página de becarios postdoctorales visibles
     */
    @Query(value = "SELECT bp.id, bp.idInstitucion, bp.fundingSource, bp.idTipoSector, bp.resources, " +
           "pr.idDescripcion, pr.idComentario, pr.fechaInicio, pr.fechaTermino, " +
           "pr.idTipoProducto, pr.urlDocumento, pr.linkVisualizacion, pr.linkPDF, pr.progressReport, " +
           "pr.idEstadoProducto, pr.codigoANID, pr.basal, pr.nameResearchLine, pr.created_at, pr.updated_at, " +
           "pr.username, " +
           "f_getRRHHProducto(bp.id) as participantesNombres, " +
           "f_getParticipantByRol(bp.id, 19) as postdoctoralFellowName " +
           "FROM becariospostdoctorales bp " +
           "INNER JOIN producto pr ON bp.id = pr.id " +
           "WHERE f_productIsVisible(bp.id, pr.username, :idRRHH, :userName) = 1",
           countQuery = "SELECT COUNT(*) FROM becariospostdoctorales bp INNER JOIN producto pr ON bp.id = pr.id WHERE f_productIsVisible(bp.id, pr.username, :idRRHH, :userName) = 1",
           nativeQuery = true)
    Page<BecariosPostdoctorales> findVisibleByUserIdRRHH(@Param("idRRHH") Long idRRHH, @Param("userName") String userName, Pageable pageable);
    
    /**
     * Obtiene un becario postdoctoral visible para un usuario específico usando f_productIsVisible
     * Hace JOIN con producto para obtener todas las columnas (herencia JOINED)
     * Incluye participantesNombres usando la función MySQL f_getRRHHProducto
     * @param id ID del becario postdoctoral
     * @param idRRHH ID del recurso humano del usuario
     * @return Optional con el becario postdoctoral si es visible, o vacío si no lo es
     */
    @Query(value = "SELECT bp.id, bp.idInstitucion, bp.fundingSource, bp.idTipoSector, bp.resources, " +
           "pr.idDescripcion, pr.idComentario, pr.fechaInicio, pr.fechaTermino, " +
           "pr.idTipoProducto, pr.urlDocumento, pr.linkVisualizacion, pr.linkPDF, pr.progressReport, " +
           "pr.idEstadoProducto, pr.codigoANID, pr.basal, pr.nameResearchLine, pr.created_at, pr.updated_at, " +
           "pr.username, " +
           "f_getRRHHProducto(bp.id) as participantesNombres, " +
           "f_getParticipantByRol(bp.id, 19) as postdoctoralFellowName " +
           "FROM becariospostdoctorales bp " +
           "INNER JOIN producto pr ON bp.id = pr.id " +
           "WHERE bp.id = :id AND f_productIsVisible(bp.id, pr.username, :idRRHH, :userName) = 1",
           nativeQuery = true)
    Optional<BecariosPostdoctorales> findVisibleByIdAndUserIdRRHH(@Param("id") Long id, @Param("idRRHH") Long idRRHH, @Param("userName") String userName);
}










