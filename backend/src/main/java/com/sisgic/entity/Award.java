package com.sisgic.entity;

import jakarta.persistence.*;

/**
 * Scientific product subtype mapped to table {@code award}.
 * Shares primary key with {@code producto}. idTipoProducto = 21.
 */
@Entity
@Table(name = "award")
@PrimaryKeyJoinColumn(name = "id")
public class Award extends ProductoCientifico {

    @Column(name = "year", nullable = false)
    private Integer year;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idinstitucion", nullable = false)
    private Institucion institucion;

    public Award() {}

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Institucion getInstitucion() {
        return institucion;
    }

    public void setInstitucion(Institucion institucion) {
        this.institucion = institucion;
    }
}
