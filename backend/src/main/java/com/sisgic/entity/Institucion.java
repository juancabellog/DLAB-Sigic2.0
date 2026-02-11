package com.sisgic.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "v_institucion")
public class Institucion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "idDescripcion", nullable = false, length = 100)
    private String idDescripcion;
    
    @Column(name = "descripcion")
    private String descripcion;

    // Constructors
    public Institucion() {}
    
    public Institucion(String idDescripcion) {
        this.idDescripcion = idDescripcion;
    }
    
    // Getters and Setters
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
    public String getDescripcion() {
        return descripcion;
    }
    
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

}










