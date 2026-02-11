package com.sisgic.repository;

import com.sisgic.dto.JournalDTO;
import com.sisgic.entity.Journal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JournalRepository extends JpaRepository<Journal, Long> {
    
    /**
     * Obtiene todos los journals con descripción resuelta desde textos
     * @param language Idioma para la descripción (por defecto 'us')
     * @return Lista de JournalDTO con descripción resuelta
     */
    @Query("""
        select new com.sisgic.dto.JournalDTO(
            j.id,
            j.idDescripcion,
            t.valor,
            j.abbreviation,
            j.issn,
            j.createdAt,
            j.updatedAt
        )
        from Journal j
        left join Textos t
          on t.id.codigoTexto = j.idDescripcion
         and t.id.idTipoTexto = 2
         and t.id.lenguaje = :language
        order by j.id asc
        """)
    List<JournalDTO> findAllByLanguage(@Param("language") String language);
    
    /**
     * Busca un journal por ID con descripción resuelta
     * @param id ID del journal
     * @param language Idioma para la descripción (por defecto 'us')
     * @return JournalDTO con descripción resuelta
     */
    @Query("""
        select new com.sisgic.dto.JournalDTO(
            j.id,
            j.idDescripcion,
            t.valor,
            j.abbreviation,
            j.issn,
            j.createdAt,
            j.updatedAt
        )
        from Journal j
        left join Textos t
          on t.id.codigoTexto = j.idDescripcion
         and t.id.idTipoTexto = 2
         and t.id.lenguaje = :language
        where j.id = :id
        """)
    Optional<JournalDTO> findByIdWithDescription(@Param("id") Long id, @Param("language") String language);
    
    /**
     * Busca journals por descripción (case insensitive) con descripción resuelta
     * @param descripcion Texto a buscar en la descripción
     * @param language Idioma para la descripción (por defecto 'us')
     * @return Lista de JournalDTO con descripción resuelta
     */
    @Query("""
        select new com.sisgic.dto.JournalDTO(
            j.id,
            j.idDescripcion,
            t.valor,
            j.abbreviation,
            j.issn,
            j.createdAt,
            j.updatedAt
        )
        from Journal j
        left join Textos t
          on t.id.codigoTexto = j.idDescripcion
         and t.id.idTipoTexto = 2
         and t.id.lenguaje = :language
        where LOWER(t.valor) like LOWER(CONCAT('%', :descripcion, '%'))
        order by j.id asc
        """)
    List<JournalDTO> findByDescripcionContainingIgnoreCase(@Param("descripcion") String descripcion, @Param("language") String language);
    
    /**
     * Busca journals por ISSN
     */
    List<Journal> findByIssn(String issn);
}











