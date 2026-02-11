package com.sisgic.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "v_pais")
public class Pais {
    
    @Id
    @Column(name = "codigo", length = 3, columnDefinition = "CHAR(3)")
    private String codigo;
    
    @Column(name = "idDescripcion", nullable = false, length = 100)
    private String idDescripcion;
    
    // Constructors
    public Pais() {}
    
    public Pais(String codigo, String idDescripcion) {
        this.codigo = codigo;
        this.idDescripcion = idDescripcion;
    }
    
    // Getters and Setters
    public String getCodigo() {
        return codigo;
    }
    
    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }
    
    public String getIdDescripcion() {
        return idDescripcion;
    }
    
    public void setIdDescripcion(String idDescripcion) {
        this.idDescripcion = idDescripcion;
    }
}


