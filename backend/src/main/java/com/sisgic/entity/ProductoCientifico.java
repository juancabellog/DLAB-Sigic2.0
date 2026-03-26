package com.sisgic.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.Formula;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "producto")
@Inheritance(strategy = InheritanceType.JOINED)
@EntityListeners(AuditingEntityListener.class)
public abstract class ProductoCientifico {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "idDescripcion")
    private String descripcion;
    
    @Column(name = "idComentario")
    private String comentario;
    
    @Column(name = "fechaInicio")
    private LocalDate fechaInicio;
    
    @Column(name = "fechaTermino")
    private LocalDate fechaTermino;
    
    @ManyToOne
    @JoinColumn(name = "idTipoProducto")
    private TipoProducto tipoProducto;
    
    @Column(name = "urlDocumento")
    private String urlDocumento;
    
    @Column(name = "linkVisualizacion")
    private String linkVisualizacion;
    
    @Column(name = "linkPDF")
    private String linkPDF;
    
    @Column(name = "progressReport")
    private String progressReport;
    
    @ManyToOne
    @JoinColumn(name = "idEstadoProducto")
    private EstadoProducto estadoProducto;
    
    @Column(name = "codigoANID")
    private String codigoANID;
    
    @Column(name = "basal")
    private Character basal;
    
    // Clusters como lista de IDs separados por coma (1-5)
    @Lob
    @Column(name = "cluster")
    private String cluster;
    
    // RELACIONES CON PROPIEDADES
    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ParticipacionProducto> participantes = new ArrayList<>();
    
    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ProductoProyecto> proyectos = new ArrayList<>();
    
    // Líneas de investigación como JSON (selección múltiple)
    @Column(name = "nameResearchLine")
    private String lineasInvestigacion; // JSON string
    
    // Campo calculado: lista de nombres de participantes concatenados por coma
    @Formula("(SELECT f_getRRHHProducto(id))")
    private String participantesNombres;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "username", nullable = true)
    private String username;
        
    
    // Constructors
    public ProductoCientifico() {}
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
    
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
    
    public String getComentario() {
        return comentario;
    }
    
    public void setComentario(String comentario) {
        this.comentario = comentario;
    }
    
    public LocalDate getFechaInicio() {
        return fechaInicio;
    }
    
    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }
    
    public LocalDate getFechaTermino() {
        return fechaTermino;
    }
    
    public void setFechaTermino(LocalDate fechaTermino) {
        this.fechaTermino = fechaTermino;
    }
    
    public TipoProducto getTipoProducto() {
        return tipoProducto;
    }
    
    public void setTipoProducto(TipoProducto tipoProducto) {
        this.tipoProducto = tipoProducto;
    }
    
    public String getUrlDocumento() {
        return urlDocumento;
    }
    
    public void setUrlDocumento(String urlDocumento) {
        this.urlDocumento = urlDocumento;
    }
    
    public String getLinkVisualizacion() {
        return linkVisualizacion;
    }
    
    public void setLinkVisualizacion(String linkVisualizacion) {
        this.linkVisualizacion = linkVisualizacion;
    }
    
    public String getLinkPDF() {
        return linkPDF;
    }
    
    public void setLinkPDF(String linkPDF) {
        this.linkPDF = linkPDF;
    }
    
    public String getProgressReport() {
        return progressReport;
    }
    
    public void setProgressReport(String progressReport) {
        this.progressReport = progressReport;
    }
    
    public EstadoProducto getEstadoProducto() {
        return estadoProducto;
    }
    
    public void setEstadoProducto(EstadoProducto estadoProducto) {
        this.estadoProducto = estadoProducto;
    }
    
    public String getCodigoANID() {
        return codigoANID;
    }
    
    public void setCodigoANID(String codigoANID) {
        this.codigoANID = codigoANID;
    }
    
    public Character getBasal() {
        return basal;
    }
    
    public void setBasal(Character basal) {
        this.basal = basal;
    }
    
    public String getCluster() {
        return cluster;
    }
    
    public void setCluster(String cluster) {
        this.cluster = cluster;
    }
    
    @JsonIgnore
    public List<ParticipacionProducto> getParticipantes() {
        return participantes;
    }
    
    public void setParticipantes(List<ParticipacionProducto> participantes) {
        this.participantes = participantes;
    }
    
    @JsonIgnore
    public List<ProductoProyecto> getProyectos() {
        return proyectos;
    }
    
    public void setProyectos(List<ProductoProyecto> proyectos) {
        this.proyectos = proyectos;
    }
    
    public String getLineasInvestigacion() {
        return lineasInvestigacion;
    }
    
    public void setLineasInvestigacion(String lineasInvestigacion) {
        this.lineasInvestigacion = lineasInvestigacion;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public String getParticipantesNombres() {
        return participantesNombres;
    }
    
    public void setParticipantesNombres(String participantesNombres) {
        this.participantesNombres = participantesNombres;
    }
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
}