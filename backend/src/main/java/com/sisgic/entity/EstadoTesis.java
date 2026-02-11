package com.sisgic.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "v_estado_tesis")
public class EstadoTesis {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "idDescripcion")
    private String idDescripcion;
    
    @Column(name = "descripcion")
    private String descripcion;
    
    // Constructors
    public EstadoTesis() {}
    
    public EstadoTesis(String idDescripcion, String descripcion) {
        this.idDescripcion = idDescripcion;
        this.descripcion = descripcion;
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










