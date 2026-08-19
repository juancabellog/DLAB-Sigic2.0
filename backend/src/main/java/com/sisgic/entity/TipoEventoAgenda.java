package com.sisgic.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "TipoEventoAgenda")
public class TipoEventoAgenda {

    @Id
    private Long id;

    @Column(name = "idDescripcion", length = 100)
    private String idDescripcion;

    public TipoEventoAgenda() {}

    public TipoEventoAgenda(Long id, String idDescripcion) {
        this.id = id;
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
