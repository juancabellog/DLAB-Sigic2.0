package com.sisgic.repository;

import com.sisgic.entity.TransferenciaTecnologica;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransferenciaTecnologicaRepository extends JpaRepository<TransferenciaTecnologica, Long> {
    
    List<TransferenciaTecnologica> findAllByOrderByIdAsc();
    
    @Query("SELECT DISTINCT tt FROM TransferenciaTecnologica tt " +
           "LEFT JOIN FETCH tt.institucion " +
           "LEFT JOIN FETCH tt.tipoTransferencia " +
           "LEFT JOIN FETCH tt.pais " +
           "LEFT JOIN FETCH tt.tipoProducto " +
           "LEFT JOIN FETCH tt.estadoProducto " +
           "LEFT JOIN FETCH tt.participantes " +
           "WHERE tt.id = :id")
    Optional<TransferenciaTecnologica> findByIdWithRelations(@Param("id") Long id);
    
    @Query("SELECT DISTINCT tt FROM TransferenciaTecnologica tt " +
           "LEFT JOIN FETCH tt.institucion " +
           "LEFT JOIN FETCH tt.tipoTransferencia " +
           "LEFT JOIN FETCH tt.pais " +
           "LEFT JOIN FETCH tt.tipoProducto " +
           "LEFT JOIN FETCH tt.estadoProducto " +
           "LEFT JOIN FETCH tt.participantes")
    List<TransferenciaTecnologica> findAllWithRelations();
    
    /**
     * Obtiene transferencias tecnológicas visibles para un usuario específico usando f_productIsVisible
     * Hace JOIN con producto para obtener todas las columnas (herencia JOINED)
     * Incluye participantesNombres usando la función MySQL f_getRRHHProducto
     * @param idRRHH ID del recurso humano del usuario (puede ser null)
     * @param pageable Paginación
     * @return Página de transferencias tecnológicas visibles
     */
    @Query(value = "SELECT tt.id, tt.idInstitucion, tt.idTipoTransferencia, tt.categoriaTransferencia, " +
           "tt.ciudad, tt.region, tt.year, tt.codigoPais, " +
           "pr.idDescripcion, pr.idComentario, pr.fechaInicio, pr.fechaTermino, " +
           "pr.idTipoProducto, pr.urlDocumento, pr.linkVisualizacion, pr.linkPDF, pr.progressReport, " +
           "pr.idEstadoProducto, pr.codigoANID, pr.basal, pr.nameResearchLine, pr.cluster, pr.created_at, pr.updated_at, " +
           "pr.username, " +
           "f_getRRHHProducto(tt.id) as participantesNombres " +
           "FROM transferenciatecnologica tt " +
           "INNER JOIN producto pr ON tt.id = pr.id " +
           "WHERE f_productIsVisible(tt.id, pr.username, :idRRHH, :userName) = 1",
           countQuery = "SELECT COUNT(*) FROM transferenciatecnologica tt INNER JOIN producto pr ON tt.id = pr.id WHERE f_productIsVisible(tt.id, pr.username, :idRRHH, :userName) = 1",
           nativeQuery = true)
    Page<TransferenciaTecnologica> findVisibleByUserIdRRHH(@Param("idRRHH") Long idRRHH, @Param("userName") String userName, Pageable pageable);
    
    /**
     * Obtiene una transferencia tecnológica visible para un usuario específico usando f_productIsVisible
     * Hace JOIN con producto para obtener todas las columnas (herencia JOINED)
     * Incluye participantesNombres usando la función MySQL f_getRRHHProducto
     * @param id ID de la transferencia tecnológica
     * @param idRRHH ID del recurso humano del usuario
     * @return Optional con la transferencia tecnológica si es visible, o vacío si no lo es
     */
    @Query(value = "SELECT tt.id, tt.idInstitucion, tt.idTipoTransferencia, tt.categoriaTransferencia, " +
           "tt.ciudad, tt.region, tt.year, tt.codigoPais, " +
           "pr.idDescripcion, pr.idComentario, pr.fechaInicio, pr.fechaTermino, " +
           "pr.idTipoProducto, pr.urlDocumento, pr.linkVisualizacion, pr.linkPDF, pr.progressReport, " +
           "pr.idEstadoProducto, pr.codigoANID, pr.basal, pr.nameResearchLine, pr.cluster, pr.created_at, pr.updated_at, " +
           "pr.username, " +
           "f_getRRHHProducto(tt.id) as participantesNombres " +
           "FROM transferenciatecnologica tt " +
           "INNER JOIN producto pr ON tt.id = pr.id " +
           "WHERE tt.id = :id AND f_productIsVisible(tt.id, pr.username, :idRRHH, :userName) = 1",
           nativeQuery = true)
    Optional<TransferenciaTecnologica> findVisibleByIdAndUserIdRRHH(@Param("id") Long id, @Param("idRRHH") Long idRRHH, @Param("userName") String userName);
}










