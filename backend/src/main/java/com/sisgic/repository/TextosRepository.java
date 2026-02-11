package com.sisgic.repository;

import com.sisgic.entity.Textos;
import com.sisgic.entity.TextosId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TextosRepository extends JpaRepository<Textos, TextosId> {
    
    // Buscar texto por código y tipo
    @Query("SELECT t FROM Textos t WHERE t.id.codigoTexto = :codigoTexto AND t.id.idTipoTexto = :idTipoTexto")
    List<Textos> findByCodigoTextoAndTipoTexto(@Param("codigoTexto") String codigoTexto, @Param("idTipoTexto") Integer idTipoTexto);
    
    // Buscar texto específico por código, tipo y lenguaje
    @Query("SELECT t FROM Textos t WHERE t.id.codigoTexto = :codigoTexto AND t.id.idTipoTexto = :idTipoTexto AND t.id.lenguaje = :lenguaje")
    Optional<Textos> findByCodigoTextoAndTipoTextoAndLenguaje(@Param("codigoTexto") String codigoTexto, @Param("idTipoTexto") Integer idTipoTexto, @Param("lenguaje") String lenguaje);
    
    // Generar próximo código de texto único
    @Query("SELECT MAX(CAST(SUBSTRING(t.id.codigoTexto, 4) AS long)) FROM Textos t WHERE t.id.codigoTexto LIKE 'TXT%' AND t.id.idTipoTexto = :idTipoTexto")
    Optional<Long> findMaxCodigoTexto(@Param("idTipoTexto") Integer idTipoTexto);
    
    // Buscar múltiples textos de una vez (para optimizar consultas en listas)
    @Query("SELECT t FROM Textos t WHERE t.id.codigoTexto IN :codigosTexto AND t.id.idTipoTexto = :idTipoTexto AND t.id.lenguaje = :lenguaje")
    List<Textos> findByCodigosTextoAndTipoTextoAndLenguaje(@Param("codigosTexto") List<String> codigosTexto, @Param("idTipoTexto") Integer idTipoTexto, @Param("lenguaje") String lenguaje);
}



