package com.sisgic.repository;

import com.sisgic.entity.Publicacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PublicacionRepository extends JpaRepository<Publicacion, Long> {
    
    /**
     * Obtiene todas las publicaciones ordenadas por ID
     */
    List<Publicacion> findAllByOrderByIdAsc();
    
    /**
     * Busca publicaciones por DOI
     */
    Optional<Publicacion> findByDoi(String doi);
    
    /**
     * Busca publicaciones por año de publicación
     */
    List<Publicacion> findByYearPublished(Integer year);
    
    /**
     * Busca publicaciones por journal
     */
    List<Publicacion> findByJournalId(Long journalId);
    
    /**
     * Query para obtener publicaciones con relaciones cargadas
     */
    @Query("SELECT DISTINCT p FROM Publicacion p " +
           "LEFT JOIN FETCH p.journal " +
           "LEFT JOIN FETCH p.tipoProducto " +
           "LEFT JOIN FETCH p.estadoProducto " +
           "LEFT JOIN FETCH p.participantes " +
           "WHERE p.id = :id")
    Optional<Publicacion> findByIdWithRelations(@Param("id") Long id);
    
    /**
     * Query para obtener todas las publicaciones con relaciones cargadas
     */
    @Query("SELECT DISTINCT p FROM Publicacion p " +
           "LEFT JOIN FETCH p.journal " +
           "LEFT JOIN FETCH p.tipoProducto " +
           "LEFT JOIN FETCH p.estadoProducto")
    List<Publicacion> findAllWithRelations();
    
    /**
     * Obtiene los factores de impacto usando las funciones de MySQL
     * @param journalId ID del journal
     * @param year Año de publicación
     * @return Resultado con los factores de impacto o null si no se encuentra
     */
    @Query(value = "SELECT f_getImpactFactor(:journalId, :year) as impactFactor, " +
                   "f_getAvgImpactFactor(:journalId, :year) as avgImpactFactor " +
                   "FROM DUAL",
           nativeQuery = true)
    ImpactFactorsResult getImpactFactors(@Param("journalId") Long journalId, 
                                         @Param("year") Integer year);
    
    /**
     * Obtiene publicaciones visibles para un usuario específico usando f_productIsVisible
     * Si idRRHH es null, la función retorna 1 (muestra todos los productos)
     * Hace JOIN con producto para obtener todas las columnas (herencia JOINED)
     * Incluye participantesNombres usando la función MySQL f_getRRHHProducto
     * @param idRRHH ID del recurso humano del usuario (puede ser null)
     * @param pageable Paginación
     * @return Página de publicaciones visibles
     */
    @Query(value = "SELECT p.id, p.idJournal, p.volume, p.yearPublished, p.firstpage, p.lastpage, " +
           "p.indexs, p.funding, p.doi, p.numCitas, p.impactFactor, p.avgImpactFactor, " +
           "pr.idDescripcion, pr.idComentario, pr.fechaInicio, pr.fechaTermino, " +
           "pr.idTipoProducto, pr.urlDocumento, pr.linkVisualizacion, pr.linkPDF, pr.progressReport, " +
           "pr.idEstadoProducto, pr.codigoANID, pr.basal, pr.nameResearchLine, pr.created_at, pr.updated_at, " +
           "pr.username, " +
           "f_getRRHHProducto(p.id) as participantesNombres " +
           "FROM publicacion p " +
           "INNER JOIN producto pr ON p.id = pr.id " +
           "WHERE f_productIsVisible(p.id, pr.username, :idRRHH, :userName) = 1",
           countQuery = "SELECT COUNT(*) FROM publicacion p INNER JOIN producto pr ON p.id = pr.id WHERE f_productIsVisible(p.id, pr.username, :idRRHH, :userName) = 1",
           nativeQuery = true)
    Page<Publicacion> findVisibleByUserIdRRHH(@Param("idRRHH") Long idRRHH, @Param("userName") String userName, Pageable pageable);
    
    /**
     * Obtiene una publicación visible para un usuario específico usando f_productIsVisible
     * Hace JOIN con producto para obtener todas las columnas (herencia JOINED)
     * Incluye participantesNombres usando la función MySQL f_getRRHHProducto
     * @param id ID de la publicación
     * @param idRRHH ID del recurso humano del usuario
     * @return Optional con la publicación si es visible, o vacío si no lo es
     */
    @Query(value = "SELECT p.id, p.idJournal, p.volume, p.yearPublished, p.firstpage, p.lastpage, " +
           "p.indexs, p.funding, p.doi, p.numCitas, p.impactFactor, p.avgImpactFactor, " +
           "pr.idDescripcion, pr.idComentario, pr.fechaInicio, pr.fechaTermino, " +
           "pr.idTipoProducto, pr.urlDocumento, pr.linkVisualizacion, pr.linkPDF, pr.progressReport, " +
           "pr.idEstadoProducto, pr.codigoANID, pr.basal, pr.nameResearchLine, pr.created_at, pr.updated_at, " +
           "pr.username, " +
           "f_getRRHHProducto(p.id) as participantesNombres " +
           "FROM publicacion p " +
           "INNER JOIN producto pr ON p.id = pr.id " +
           "WHERE p.id = :id AND f_productIsVisible(p.id, pr.username, :idRRHH, :userName) = 1",
           nativeQuery = true)
    Optional<Publicacion> findVisibleByIdAndUserIdRRHH(@Param("id") Long id, @Param("idRRHH") Long idRRHH, @Param("userName") String userName);
    
    /**
     * Interfaz para mapear los resultados de la consulta de factores de impacto
     */
    interface ImpactFactorsResult {
        BigDecimal getImpactFactor();
        BigDecimal getAvgImpactFactor();
    }
}

