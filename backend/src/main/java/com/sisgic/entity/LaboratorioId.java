package com.sisgic.entity;

import java.io.Serializable;
import java.util.Objects;

public class LaboratorioId implements Serializable {

    private String codigoCentro;
    private Long id;
    private Long idArea;

    public LaboratorioId() {}

    public LaboratorioId(String codigoCentro, Long id, Long idArea) {
        this.codigoCentro = codigoCentro;
        this.id = id;
        this.idArea = idArea;
    }

    public String getCodigoCentro() { return codigoCentro; }
    public void setCodigoCentro(String codigoCentro) { this.codigoCentro = codigoCentro; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getIdArea() { return idArea; }
    public void setIdArea(Long idArea) { this.idArea = idArea; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LaboratorioId that = (LaboratorioId) o;
        return Objects.equals(codigoCentro, that.codigoCentro)
            && Objects.equals(id, that.id)
            && Objects.equals(idArea, that.idArea);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigoCentro, id, idArea);
    }
}
