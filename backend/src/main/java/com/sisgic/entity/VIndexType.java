package com.sisgic.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Read-only entity mapped to the DB view v_index_type.
 * Expected columns: id, descripcion.
 */
@Entity
@Table(name = "v_index_type")
public class VIndexType {

    @Id
    private Long id;

    @Column(name = "descripcion")
    private String descripcion;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}

