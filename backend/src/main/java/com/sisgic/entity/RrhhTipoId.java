package com.sisgic.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

public class RrhhTipoId implements Serializable {

    private Long idRRHH;
    private Long idTipoRRHH;
    private LocalDate fechaInicio;

    public RrhhTipoId() {}

    public RrhhTipoId(Long idRRHH, Long idTipoRRHH, LocalDate fechaInicio) {
        this.idRRHH = idRRHH;
        this.idTipoRRHH = idTipoRRHH;
        this.fechaInicio = fechaInicio;
    }

    public Long getIdRRHH() { return idRRHH; }
    public void setIdRRHH(Long idRRHH) { this.idRRHH = idRRHH; }

    public Long getIdTipoRRHH() { return idTipoRRHH; }
    public void setIdTipoRRHH(Long idTipoRRHH) { this.idTipoRRHH = idTipoRRHH; }

    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RrhhTipoId that = (RrhhTipoId) o;
        return Objects.equals(idRRHH, that.idRRHH)
            && Objects.equals(idTipoRRHH, that.idTipoRRHH)
            && Objects.equals(fechaInicio, that.fechaInicio);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idRRHH, idTipoRRHH, fechaInicio);
    }
}
