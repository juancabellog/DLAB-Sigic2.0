package com.sisgic.entity;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "agenda")
@PrimaryKeyJoinColumn(name = "id")
public class Agenda extends ProductoCientifico {

    public static final int ID_TIPO_PRODUCTO = 18;
    public static final int ID_TIPO_TEXTO = 2;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "idEstado")
    private EstadoNoticia estado;

    @Column(name = "startTime", length = 5, columnDefinition = "CHAR(5)", nullable = false)
    private String startTime;

    @Column(name = "endTime", length = 5, columnDefinition = "CHAR(5)")
    private String endTime;

    @Column(name = "lugar", columnDefinition = "TEXT")
    private String lugar;

    @Column(name = "image", columnDefinition = "LONGTEXT")
    private String image;

    @Column(name = "feature", length = 1, columnDefinition = "CHAR(1)", nullable = false)
    private Character feature = 'N';

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "idTipoEvento", nullable = false)
    private TipoEventoAgenda tipoEvento;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "agenda_categoryevents",
        joinColumns = @JoinColumn(name = "idAgenda"),
        inverseJoinColumns = @JoinColumn(name = "idCategory")
    )
    private Set<CategoryEvent> categories = new HashSet<>();

    public Agenda() {}

    public EstadoNoticia getEstado() {
        return estado;
    }

    public void setEstado(EstadoNoticia estado) {
        this.estado = estado;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getLugar() {
        return lugar;
    }

    public void setLugar(String lugar) {
        this.lugar = lugar;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Character getFeature() {
        return feature;
    }

    public void setFeature(Character feature) {
        this.feature = feature;
    }

    public TipoEventoAgenda getTipoEvento() {
        return tipoEvento;
    }

    public void setTipoEvento(TipoEventoAgenda tipoEvento) {
        this.tipoEvento = tipoEvento;
    }

    public Set<CategoryEvent> getCategories() {
        return categories;
    }

    public void setCategories(Set<CategoryEvent> categories) {
        this.categories = categories != null ? categories : new HashSet<>();
    }
}
