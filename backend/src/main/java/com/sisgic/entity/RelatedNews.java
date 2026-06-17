package com.sisgic.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "related_news")
@IdClass(RelatedNewsId.class)
public class RelatedNews {

    @Id
    @Column(name = "idNoticia", nullable = false)
    private Long idNoticia;

    @Id
    @Column(name = "idNoticiaRef", nullable = false)
    private Long idNoticiaRef;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idNoticia", insertable = false, updatable = false)
    private Noticia noticia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idNoticiaRef", insertable = false, updatable = false)
    private Noticia relatedNoticia;

    public RelatedNews() {}

    public RelatedNews(Long idNoticia, Long idNoticiaRef) {
        this.idNoticia = idNoticia;
        this.idNoticiaRef = idNoticiaRef;
    }

    public Long getIdNoticia() {
        return idNoticia;
    }

    public void setIdNoticia(Long idNoticia) {
        this.idNoticia = idNoticia;
    }

    public Long getIdNoticiaRef() {
        return idNoticiaRef;
    }

    public void setIdNoticiaRef(Long idNoticiaRef) {
        this.idNoticiaRef = idNoticiaRef;
    }

    public Noticia getNoticia() {
        return noticia;
    }

    public void setNoticia(Noticia noticia) {
        this.noticia = noticia;
    }

    public Noticia getRelatedNoticia() {
        return relatedNoticia;
    }

    public void setRelatedNoticia(Noticia relatedNoticia) {
        this.relatedNoticia = relatedNoticia;
    }
}
