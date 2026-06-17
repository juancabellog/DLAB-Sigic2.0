package com.sisgic.entity;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "noticias")
@PrimaryKeyJoinColumn(name = "id")
public class Noticia extends ProductoCientifico {

    public static final int ID_TIPO_PRODUCTO = 17;
    public static final int ID_TIPO_TEXTO = 2;

    @Column(name = "idTitulo", length = 100)
    private String idTitulo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "idEstado")
    private EstadoNoticia estado;

    @Column(name = "numVisitas")
    private Integer numVisitas;

    @Column(name = "numLikes")
    private Integer numLikes;

    @Column(name = "image", columnDefinition = "LONGTEXT")
    private String image;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "noticias_tags",
        joinColumns = @JoinColumn(name = "idNoticia"),
        inverseJoinColumns = @JoinColumn(name = "idTag")
    )
    private Set<Tag> tags = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "noticias_category",
        joinColumns = @JoinColumn(name = "idNoticia"),
        inverseJoinColumns = @JoinColumn(name = "idCategory")
    )
    private Set<Category> categories = new HashSet<>();

    @Column(name = "feature", length = 1, columnDefinition = "CHAR(1)")
    private Character feature;

    public Noticia() {}

    public String getIdTitulo() {
        return idTitulo;
    }

    public void setIdTitulo(String idTitulo) {
        this.idTitulo = idTitulo;
    }

    public EstadoNoticia getEstado() {
        return estado;
    }

    public void setEstado(EstadoNoticia estado) {
        this.estado = estado;
    }

    public Integer getNumVisitas() {
        return numVisitas;
    }

    public void setNumVisitas(Integer numVisitas) {
        this.numVisitas = numVisitas;
    }

    public Integer getNumLikes() {
        return numLikes;
    }

    public void setNumLikes(Integer numLikes) {
        this.numLikes = numLikes;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Set<Tag> getTags() {
        return tags;
    }

    public void setTags(Set<Tag> tags) {
        this.tags = tags != null ? tags : new HashSet<>();
    }

    public Set<Category> getCategories() {
        return categories;
    }

    public void setCategories(Set<Category> categories) {
        this.categories = categories != null ? categories : new HashSet<>();
    }

    public Character getFeature() {
        return feature;
    }

    public void setFeature(Character feature) {
        this.feature = feature;
    }
}
