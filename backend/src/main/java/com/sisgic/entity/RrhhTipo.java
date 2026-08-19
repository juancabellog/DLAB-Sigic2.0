package com.sisgic.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "rrhh_tipo")
@IdClass(RrhhTipoId.class)
public class RrhhTipo {

    @Id
    @Column(name = "idRRHH", nullable = false)
    private Long idRRHH;

    @Id
    @Column(name = "idTipoRRHH", nullable = false)
    private Long idTipoRRHH;

    @Id
    @Column(name = "fechaInicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fechaTermino")
    private LocalDate fechaTermino;

    @Column(name = "progressReport", nullable = false)
    private Integer progressReport = 0;

    public RrhhTipo() {}

    public Long getIdRRHH() { return idRRHH; }
    public void setIdRRHH(Long idRRHH) { this.idRRHH = idRRHH; }

    public Long getIdTipoRRHH() { return idTipoRRHH; }
    public void setIdTipoRRHH(Long idTipoRRHH) { this.idTipoRRHH = idTipoRRHH; }

    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDate getFechaTermino() { return fechaTermino; }
    public void setFechaTermino(LocalDate fechaTermino) { this.fechaTermino = fechaTermino; }

    public Integer getProgressReport() { return progressReport; }
    public void setProgressReport(Integer progressReport) { this.progressReport = progressReport; }

    public boolean isActive() {
        return fechaTermino == null;
    }
}
