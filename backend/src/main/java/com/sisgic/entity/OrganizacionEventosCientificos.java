package com.sisgic.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Formula;

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
    
    @Formula("(SELECT f_getParticipantByRol(id, 14))")
    private String organizer; // Campo calculado: nombre del organizador (rol 14)
    
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
    
    public String getOrganizer() {
        return organizer;
    }
    
    public void setOrganizer(String organizer) {
        this.organizer = organizer;
    }
}

