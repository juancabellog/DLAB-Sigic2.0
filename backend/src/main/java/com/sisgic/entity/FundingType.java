package com.sisgic.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "fundingtype")
public class FundingType {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "idDescripcion")
    private String idDescripcion;
    
    // Constructors
    public FundingType() {}
    
    public FundingType(String idDescripcion) {
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
}










