package com.sisgic.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "estadonoticia")
public class EstadoNoticia {

    public static final long PUBLISHED = 1L;
    public static final long DRAFT = 2L;
    public static final long UNPUBLISHED = 3L;

    @Id
    private Long id;

    @Column(name = "idDescripcion")
    private String idDescripcion;

    public EstadoNoticia() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIdDescripcion() {
        return idDescripcion;
    }

    public void setIdDescripcion(String idDescripcion) {
        this.idDescripcion = idDescripcion;
    }
}
