package com.sisgic.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "laboratorio_rrhh")
@IdClass(LaboratorioRrhhId.class)
public class LaboratorioRrhh {

    public static final String TIPO_DIRECTOR = "D";
    public static final String TIPO_LAB_MANAGER = "M";
    public static final String TIPO_MEMBER = "I";

    @Id
    @Column(name = "codigoCentro", length = 20, nullable = false)
    private String codigoCentro;

    @Id
    @Column(name = "idArea", nullable = false)
    private Long idArea;

    @Id
    @Column(name = "idLaboratorio", nullable = false)
    private Long idLaboratorio;

    @Id
    @Column(name = "idRRHH", nullable = false)
    private Long idRRHH;

    @Id
    @Column(name = "fechaInicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "tipoRecurso", length = 1, columnDefinition = "CHAR(1)", nullable = false)
    private String tipoRecurso;

    @Column(name = "fechaTermino")
    private LocalDate fechaTermino;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "idRRHH", insertable = false, updatable = false)
    private RRHH rrhh;

    public LaboratorioRrhh() {}

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

    public String getTipoRecurso() { return tipoRecurso; }
    public void setTipoRecurso(String tipoRecurso) { this.tipoRecurso = tipoRecurso; }

    public LocalDate getFechaTermino() { return fechaTermino; }
    public void setFechaTermino(LocalDate fechaTermino) { this.fechaTermino = fechaTermino; }

    public RRHH getRrhh() { return rrhh; }
    public void setRrhh(RRHH rrhh) { this.rrhh = rrhh; }

    public boolean isActive() {
        return fechaTermino == null;
    }

    public LaboratorioRrhhId getLaboratorioRrhhId() {
        return new LaboratorioRrhhId(codigoCentro, idArea, idLaboratorio, idRRHH, fechaInicio);
    }
}
