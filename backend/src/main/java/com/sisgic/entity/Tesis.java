package com.sisgic.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Formula;

import java.time.LocalDate;

@Entity
@Table(name = "tesis")
@PrimaryKeyJoinColumn(name = "id")
public class Tesis extends ProductoCientifico {
    
    @ManyToOne
    @JoinColumn(name = "idInstitucionOG")
    private Institucion institucionOG; // Institución que otorga el título
    
    @ManyToOne
    @JoinColumn(name = "idGradoAcademico")
    private GradoAcademico gradoAcademico;
    
    @ManyToOne
    @JoinColumn(name = "idInstitucion")
    private Institucion institucion; // Institución donde se insertó el estudiante
    
    @ManyToOne
    @JoinColumn(name = "idEstadoTesis")
    private EstadoTesis estadoTesis;
    
    @Column(name = "fechaInicioPrograma")
    private LocalDate fechaInicioPrograma;
    
    @Column(name = "nombreCompletoTitulo", nullable = false, columnDefinition = "TEXT")
    private String nombreCompletoTitulo;
    
    @Lob
    @Column(name = "tipoSector")
    private String tipoSector; // JSON string o lista separada por comas
    
    @Formula("(SELECT f_getParticipantByRol(id, 7))")
    private String estudiante; // Campo calculado: nombre del estudiante (rol 7)
    
    // Constructors
    public Tesis() {}
    
    // Getters and Setters
    public Institucion getInstitucionOG() {
        return institucionOG;
    }
    
    public void setInstitucionOG(Institucion institucionOG) {
        this.institucionOG = institucionOG;
    }
    
    public GradoAcademico getGradoAcademico() {
        return gradoAcademico;
    }
    
    public void setGradoAcademico(GradoAcademico gradoAcademico) {
        this.gradoAcademico = gradoAcademico;
    }
    
    public Institucion getInstitucion() {
        return institucion;
    }
    
    public void setInstitucion(Institucion institucion) {
        this.institucion = institucion;
    }
    
    public EstadoTesis getEstadoTesis() {
        return estadoTesis;
    }
    
    public void setEstadoTesis(EstadoTesis estadoTesis) {
        this.estadoTesis = estadoTesis;
    }
    
    public LocalDate getFechaInicioPrograma() {
        return fechaInicioPrograma;
    }
    
    public void setFechaInicioPrograma(LocalDate fechaInicioPrograma) {
        this.fechaInicioPrograma = fechaInicioPrograma;
    }
    
    public String getNombreCompletoTitulo() {
        return nombreCompletoTitulo;
    }
    
    public void setNombreCompletoTitulo(String nombreCompletoTitulo) {
        this.nombreCompletoTitulo = nombreCompletoTitulo;
    }
    
    public String getTipoSector() {
        return tipoSector;
    }
    
    public void setTipoSector(String tipoSector) {
        this.tipoSector = tipoSector;
    }
    
    public String getEstudiante() {
        return estudiante;
    }
    
    public void setEstudiante(String estudiante) {
        this.estudiante = estudiante;
    }
}

