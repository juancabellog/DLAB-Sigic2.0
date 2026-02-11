package com.sisgic.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "colaboraciones")
@PrimaryKeyJoinColumn(name = "id")
public class Colaboracion extends ProductoCientifico {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idTipoColaboracion")
    private TipoColaboracion tipoColaboracion;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idInstitucion")
    private Institucion institucion;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "codigoPaisOrigen")
    private Pais paisOrigen;
    
    @Column(name = "ciudadOrigen", nullable = false, length = 100)
    private String ciudadOrigen;
    
    @Column(name = "codigoPaisDestino", columnDefinition = "CHAR(3)")
    private String codigoPaisDestino;
    
    @Column(name = "ciudadDestino", nullable = false, length = 100)
    private String ciudadDestino;
    
    // Getters and Setters
    public TipoColaboracion getTipoColaboracion() {
        return tipoColaboracion;
    }
    
    public void setTipoColaboracion(TipoColaboracion tipoColaboracion) {
        this.tipoColaboracion = tipoColaboracion;
    }
    
    public Institucion getInstitucion() {
        return institucion;
    }
    
    public void setInstitucion(Institucion institucion) {
        this.institucion = institucion;
    }
    
    public Pais getPaisOrigen() {
        return paisOrigen;
    }
    
    public void setPaisOrigen(Pais paisOrigen) {
        this.paisOrigen = paisOrigen;
    }
    
    public String getCiudadOrigen() {
        return ciudadOrigen;
    }
    
    public void setCiudadOrigen(String ciudadOrigen) {
        this.ciudadOrigen = ciudadOrigen;
    }
    
    public String getCodigoPaisDestino() {
        return codigoPaisDestino;
    }
    
    public void setCodigoPaisDestino(String codigoPaisDestino) {
        this.codigoPaisDestino = codigoPaisDestino;
    }
    
    public String getCiudadDestino() {
        return ciudadDestino;
    }
    
    public void setCiudadDestino(String ciudadDestino) {
        this.ciudadDestino = ciudadDestino;
    }
}

