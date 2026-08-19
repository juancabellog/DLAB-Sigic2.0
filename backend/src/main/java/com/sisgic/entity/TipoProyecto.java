package com.sisgic.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "tipoproyecto")
public class TipoProyecto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idDescripcion", nullable = false, length = 200)
    private String idDescripcion;

    public TipoProyecto() {}

    public TipoProyecto(String idDescripcion) {
        this.idDescripcion = idDescripcion;
    }

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
