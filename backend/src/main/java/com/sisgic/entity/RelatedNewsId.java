package com.sisgic.entity;

import java.io.Serializable;
import java.util.Objects;

public class RelatedNewsId implements Serializable {

    private Long idNoticia;
    private Long idNoticiaRef;

    public RelatedNewsId() {}

    public RelatedNewsId(Long idNoticia, Long idNoticiaRef) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RelatedNewsId that = (RelatedNewsId) o;
        return Objects.equals(idNoticia, that.idNoticia)
            && Objects.equals(idNoticiaRef, that.idNoticiaRef);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idNoticia, idNoticiaRef);
    }
}
