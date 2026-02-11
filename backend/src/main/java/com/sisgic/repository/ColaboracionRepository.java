package com.sisgic.repository;

import com.sisgic.entity.Colaboracion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ColaboracionRepository extends JpaRepository<Colaboracion, Long> {
    
    @Query("SELECT c FROM Colaboracion c " +
           "LEFT JOIN FETCH c.tipoColaboracion " +
           "LEFT JOIN FETCH c.institucion " +
           "LEFT JOIN FETCH c.paisOrigen " +
           "LEFT JOIN FETCH c.tipoProducto " +
           "LEFT JOIN FETCH c.estadoProducto " +
           "WHERE c.id = :id")
    Optional<Colaboracion> findByIdWithRelations(@Param("id") Long id);
    
    /**
     * Obtiene colaboraciones visibles para un usuario específico usando f_productIsVisible
     * Hace JOIN con producto para obtener todas las columnas (herencia JOINED)
     * Incluye participantesNombres usando la función MySQL f_getRRHHProducto
     * @param idRRHH ID del recurso humano del usuario (puede ser null)
     * @param pageable Paginación
     * @return Página de colaboraciones visibles
     */
    @Query(value = "SELECT c.id, c.idTipoColaboracion, c.idInstitucion, c.codigoPaisOrigen, c.ciudadOrigen, " +
           "c.codigoPaisDestino, c.ciudadDestino, " +
           "pr.idDescripcion, pr.idComentario, pr.fechaInicio, pr.fechaTermino, " +
           "pr.idTipoProducto, pr.urlDocumento, pr.linkVisualizacion, pr.linkPDF, pr.progressReport, " +
           "pr.idEstadoProducto, pr.codigoANID, pr.basal, pr.nameResearchLine, pr.created_at, pr.updated_at, " +
           "pr.username, " +
           "f_getRRHHProducto(c.id) as participantesNombres " +
           "FROM colaboraciones c " +
           "INNER JOIN producto pr ON c.id = pr.id " +
           "WHERE f_productIsVisible(c.id, pr.username, :idRRHH, :userName) = 1",
           countQuery = "SELECT COUNT(*) FROM colaboraciones c INNER JOIN producto pr ON c.id = pr.id WHERE f_productIsVisible(c.id, pr.username, :idRRHH, :userName) = 1",
           nativeQuery = true)
    Page<Colaboracion> findVisibleByUserIdRRHH(@Param("idRRHH") Long idRRHH, @Param("userName") String userName, Pageable pageable);
    
    /**
     * Obtiene una colaboración visible para un usuario específico usando f_productIsVisible
     * Hace JOIN con producto para obtener todas las columnas (herencia JOINED)
     * Incluye participantesNombres usando la función MySQL f_getRRHHProducto
     * @param id ID de la colaboración
     * @param idRRHH ID del recurso humano del usuario
     * @return Optional con la colaboración si es visible, o vacío si no lo es
     */
    @Query(value = "SELECT c.id, c.idTipoColaboracion, c.idInstitucion, c.codigoPaisOrigen, c.ciudadOrigen, " +
           "c.codigoPaisDestino, c.ciudadDestino, " +
           "pr.idDescripcion, pr.idComentario, pr.fechaInicio, pr.fechaTermino, " +
           "pr.idTipoProducto, pr.urlDocumento, pr.linkVisualizacion, pr.linkPDF, pr.progressReport, " +
           "pr.idEstadoProducto, pr.codigoANID, pr.basal, pr.nameResearchLine, pr.created_at, pr.updated_at, " +
           "pr.username, " +
           "f_getRRHHProducto(c.id) as participantesNombres " +
           "FROM colaboraciones c " +
           "INNER JOIN producto pr ON c.id = pr.id " +
           "WHERE c.id = :id AND f_productIsVisible(c.id, pr.username, :idRRHH, :userName) = 1",
           nativeQuery = true)
    Optional<Colaboracion> findVisibleByIdAndUserIdRRHH(@Param("id") Long id, @Param("idRRHH") Long idRRHH, @Param("userName") String userName);
}

