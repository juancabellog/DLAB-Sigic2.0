package com.sisgic.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "organizacioneventoscientificos")
@PrimaryKeyJoinColumn(name = "id")
public class OrganizacionEventosCientificos extends ProductoCientifico {
    
    @ManyToOne
    @JoinColumn(name = "idTipoEvento")
    private TipoEvento tipoEvento;
    
    @ManyToOne
    @JoinColumn(name = "codigoPais")
    private Pais pais;
    
    @Column(name = "ciudad", nullable = false, length = 100)
    private String ciudad;
    
    @Column(name = "numParticipantes")
    private Integer numParticipantes;
    
    // Constructors
    public OrganizacionEventosCientificos() {}
    
    // Getters and Setters
    public TipoEvento getTipoEvento() {
        return tipoEvento;
    }
    
    public void setTipoEvento(TipoEvento tipoEvento) {
        this.tipoEvento = tipoEvento;
    }
    
    public Pais getPais() {
        return pais;
    }
    
    public void setPais(Pais pais) {
        this.pais = pais;
    }
    
    public String getCiudad() {
        return ciudad;
    }
    
    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }
    
    public Integer getNumParticipantes() {
        return numParticipantes;
    }
    
    public void setNumParticipantes(Integer numParticipantes) {
        this.numParticipantes = numParticipantes;
    }
}

