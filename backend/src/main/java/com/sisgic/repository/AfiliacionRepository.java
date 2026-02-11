package com.sisgic.repository;

import com.sisgic.entity.Afiliacion;
import com.sisgic.entity.AfiliacionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AfiliacionRepository extends JpaRepository<Afiliacion, AfiliacionId> {
    
    /**
     * Obtiene todas las afiliaciones ordenadas por ID
     */
    List<Afiliacion> findAllByOrderByIdAsc();
    
    /**
     * Busca afiliaciones por ID de RRHH
     */
    @Query("SELECT a FROM Afiliacion a WHERE a.id.idRRHH = :idRRHH")
    List<Afiliacion> findByIdRRHH(@Param("idRRHH") Long idRRHH);
    
    /**
     * Busca afiliaciones por ID de Producto
     */
    @Query("SELECT a FROM Afiliacion a WHERE a.id.idProducto = :idProducto")
    List<Afiliacion> findByIdProducto(@Param("idProducto") Long idProducto);
    
    /**
     * Busca afiliaciones por participación (idRRHH, idProducto, idRRHHProducto)
     * Carga la institución usando JOIN FETCH para evitar LazyInitializationException
     */
    @Query("SELECT DISTINCT a FROM Afiliacion a " +
           "LEFT JOIN FETCH a.institucion " +
           "WHERE a.id.idRRHH = :idRRHH " +
           "AND a.id.idProducto = :idProducto AND a.id.idRRHHProducto = :idRRHHProducto")
    List<Afiliacion> findByParticipacion(@Param("idRRHH") Long idRRHH,
                                          @Param("idProducto") Long idProducto,
                                          @Param("idRRHHProducto") Long idRRHHProducto);
    
    /**
     * Busca afiliaciones por ID de Institución
     */
    @Query("SELECT a FROM Afiliacion a WHERE a.institucion.id = :idInstitucion")
    List<Afiliacion> findByIdInstitucion(@Param("idInstitucion") Long idInstitucion);
    
    /**
     * Obtiene el siguiente ID correlativo para una combinación de (idProducto, idRRHH, idRRHHProducto)
     * El id es un correlativo que se incrementa para cada combinación única
     */
    @Query(value = "SELECT COALESCE(MAX(id), 0) + 1 FROM afiliacion " +
                   "WHERE idProducto = :productoId AND idRRHH = :rrhhId AND idRRHHProducto = :rrhhProductoId",
           nativeQuery = true)
    Long getNextIdForAfiliacion(@Param("productoId") Long productoId,
                                 @Param("rrhhId") Long rrhhId,
                                 @Param("rrhhProductoId") Long rrhhProductoId);
}

