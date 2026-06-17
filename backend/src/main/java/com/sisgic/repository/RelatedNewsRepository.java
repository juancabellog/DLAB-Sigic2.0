package com.sisgic.repository;

import com.sisgic.entity.RelatedNews;
import com.sisgic.entity.RelatedNewsId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RelatedNewsRepository extends JpaRepository<RelatedNews, RelatedNewsId> {

    @Query("SELECT r FROM RelatedNews r " +
           "JOIN FETCH r.relatedNoticia rn " +
           "LEFT JOIN FETCH rn.estado " +
           "WHERE r.idNoticia = :idNoticia " +
           "ORDER BY r.idNoticiaRef")
    List<RelatedNews> findByIdNoticiaWithRelated(@Param("idNoticia") Long idNoticia);

    @Query("SELECT r.idNoticiaRef FROM RelatedNews r " +
           "WHERE r.idNoticia = :idNoticia " +
           "ORDER BY r.idNoticiaRef")
    List<Long> findRelatedIdsByIdNoticia(@Param("idNoticia") Long idNoticia);

    @Modifying
    @Query("DELETE FROM RelatedNews r WHERE r.idNoticia = :idNoticia")
    void deleteByIdNoticia(@Param("idNoticia") Long idNoticia);

    @Modifying
    @Query("DELETE FROM RelatedNews r WHERE r.idNoticiaRef = :idNoticiaRef")
    void deleteByIdNoticiaRef(@Param("idNoticiaRef") Long idNoticiaRef);
}
