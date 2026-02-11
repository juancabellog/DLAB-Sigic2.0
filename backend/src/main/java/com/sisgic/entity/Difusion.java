package com.sisgic.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "difusion")
@PrimaryKeyJoinColumn(name = "id")
public class Difusion extends ProductoCientifico {
    
    @ManyToOne
    @JoinColumn(name = "idTipoDifusion")
    private TipoDifusion tipoDifusion;
    
    @ManyToOne
    @JoinColumn(name = "codigoPais")
    private Pais pais;
    
    @Column(name = "lugar", length = 200)
    private String lugar;
    
    @Column(name = "numAsistentes")
    private Integer numAsistentes;
    
    @Column(name = "duracion")
    private Integer duracion;
    
    @Lob
    @Column(name = "publicoObjetivo")
    private String publicoObjetivo; // Lista separada por comas de IDs
    
    @Column(name = "ciudad", length = 200)
    private String ciudad;
    
    @Column(name = "link", columnDefinition = "TEXT")
    private String link;
    
    // Constructors
    public Difusion() {}
    
    // Getters and Setters
    public TipoDifusion getTipoDifusion() {
        return tipoDifusion;
    }
    
    public void setTipoDifusion(TipoDifusion tipoDifusion) {
        this.tipoDifusion = tipoDifusion;
    }
    
    public Pais getPais() {
        return pais;
    }
    
    public void setPais(Pais pais) {
        this.pais = pais;
    }
    
    public String getLugar() {
        return lugar;
    }
    
    public void setLugar(String lugar) {
        this.lugar = lugar;
    }
    
    public Integer getNumAsistentes() {
        return numAsistentes;
    }
    
    public void setNumAsistentes(Integer numAsistentes) {
        this.numAsistentes = numAsistentes;
    }
    
    public Integer getDuracion() {
        return duracion;
    }
    
    public void setDuracion(Integer duracion) {
        this.duracion = duracion;
    }
    
    public String getPublicoObjetivo() {
        return publicoObjetivo;
    }
    
    public void setPublicoObjetivo(String publicoObjetivo) {
        this.publicoObjetivo = publicoObjetivo;
    }
    
    public String getCiudad() {
        return ciudad;
    }
    
    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }
    
    public String getLink() {
        return link;
    }
    
    public void setLink(String link) {
        this.link = link;
    }
}


