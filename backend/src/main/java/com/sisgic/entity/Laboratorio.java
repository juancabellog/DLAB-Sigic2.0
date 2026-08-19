package com.sisgic.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "laboratorio")
@IdClass(LaboratorioId.class)
public class Laboratorio {

    public static final String CODIGO_CENTRO = "SIGIC";
    public static final int ID_TIPO_TEXTO = 2;

    @Id
    @Column(name = "codigoCentro", length = 20, nullable = false)
    private String codigoCentro = CODIGO_CENTRO;

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Id
    @Column(name = "idArea", nullable = false)
    private Long idArea;

    @Column(name = "idDescripcion", length = 100, nullable = false)
    private String idDescripcion;

    @Column(name = "idComentario", length = 100)
    private String idComentario;

    @Column(name = "activo", length = 1, columnDefinition = "CHAR(1)")
    private String activo = "N";

    @Column(name = "urlImagen", columnDefinition = "LONGTEXT")
    private String urlImagen;

    public Laboratorio() {}

    public String getCodigoCentro() { return codigoCentro; }
    public void setCodigoCentro(String codigoCentro) { this.codigoCentro = codigoCentro; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getIdArea() { return idArea; }
    public void setIdArea(Long idArea) { this.idArea = idArea; }

    public String getIdDescripcion() { return idDescripcion; }
    public void setIdDescripcion(String idDescripcion) { this.idDescripcion = idDescripcion; }

    public String getIdComentario() { return idComentario; }
    public void setIdComentario(String idComentario) { this.idComentario = idComentario; }

    public String getActivo() { return activo; }
    public void setActivo(String activo) { this.activo = activo; }

    public String getUrlImagen() { return urlImagen; }
    public void setUrlImagen(String urlImagen) { this.urlImagen = urlImagen; }

    public LaboratorioId getLaboratorioId() {
        return new LaboratorioId(codigoCentro, id, idArea);
    }
}
