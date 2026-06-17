package com.sisgic.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "agenda")
@PrimaryKeyJoinColumn(name = "id")
public class Agenda extends ProductoCientifico {

    public static final int ID_TIPO_PRODUCTO = 18;
    public static final int ID_TIPO_TEXTO = 2;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "idEstado")
    private EstadoNoticia estado;

    @Column(name = "hora", length = 8, columnDefinition = "CHAR(8)")
    private String hora;

    @Column(name = "lugar", columnDefinition = "TEXT")
    private String lugar;

    @Column(name = "image", columnDefinition = "LONGTEXT")
    private String image;

    public Agenda() {}

    public EstadoNoticia getEstado() {
        return estado;
    }

    public void setEstado(EstadoNoticia estado) {
        this.estado = estado;
    }

    public String getHora() {
        return hora;
    }

    public void setHora(String hora) {
        this.hora = hora;
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
}
