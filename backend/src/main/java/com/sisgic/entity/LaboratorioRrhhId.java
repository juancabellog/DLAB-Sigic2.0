package com.sisgic.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

public class LaboratorioRrhhId implements Serializable {

    private String codigoCentro;
    private Long idArea;
    private Long idLaboratorio;
    private Long idRRHH;
    private LocalDate fechaInicio;

    public LaboratorioRrhhId() {}

    public LaboratorioRrhhId(String codigoCentro, Long idArea, Long idLaboratorio,
                             Long idRRHH, LocalDate fechaInicio) {
        this.codigoCentro = codigoCentro;
        this.idArea = idArea;
        this.idLaboratorio = idLaboratorio;
        this.idRRHH = idRRHH;
        this.fechaInicio = fechaInicio;
    }

    public String getCodigoCentro() { return codigoCentro; }
    public void setCodigoCentro(String codigoCentro) { this.codigoCentro = codigoCentro; }

    public Long getIdArea() { return idArea; }
    public void setIdArea(Long idArea) { this.idArea = idArea; }

    public Long getIdLaboratorio() { return idLaboratorio; }
    public void setIdLaboratorio(Long idLaboratorio) { this.idLaboratorio = idLaboratorio; }

    public Long getIdRRHH() { return idRRHH; }
    public void setIdRRHH(Long idRRHH) { this.idRRHH = idRRHH; }

    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LaboratorioRrhhId that = (LaboratorioRrhhId) o;
        return Objects.equals(codigoCentro, that.codigoCentro)
            && Objects.equals(idArea, that.idArea)
            && Objects.equals(idLaboratorio, that.idLaboratorio)
            && Objects.equals(idRRHH, that.idRRHH)
            && Objects.equals(fechaInicio, that.fechaInicio);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigoCentro, idArea, idLaboratorio, idRRHH, fechaInicio);
    }
}
