package com.sisgic.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.Formula;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rrhh")
@EntityListeners(AuditingEntityListener.class)
public class RRHH {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "idRecurso")
    private String idRecurso; // RUT Chile
    
    @Column(name = "primerNombre", length = 40)
    private String primerNombre;
    
    @Column(name = "segundoNombre", length = 40)
    private String segundoNombre;
    
    @Column(name = "primerApellido", length = 40)
    private String primerApellido;
    
    @Column(name = "segundoApellido", length = 40)
    private String segundoApellido;
    
    // fullname es calculado usando la función MySQL f_getFullName(idRRHH)
    // La función recibe el id del RRHH y retorna el nombre completo
    // Nota: En @Formula, 'id' se refiere al campo id de esta entidad
    @Formula("f_getFullName(id)")
    private String fullname;
    
    @ManyToOne
    @JoinColumn(name = "idTipoRRHH")
    private TipoRRHH tipoRRHH;
    
    @Column(name = "numCelular")
    private String numCelular;
    
    @Column(name = "email")
    private String email;
    
    @Column(name = "iniciales", length = 3, columnDefinition = "CHAR(3)")
    private String iniciales;
    
    @Column(name = "orcid", length = 19, columnDefinition = "CHAR(19)")
    private String orcid;
    
    @Column(name = "codigoGenero", length = 1, columnDefinition = "CHAR(1)")
    private String codigoGenero; // M o F
    
    @OneToMany(mappedBy = "rrhh", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ParticipacionProducto> participaciones = new ArrayList<>();
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public RRHH() {}
    
    public RRHH(String idRecurso, String primerNombre, String segundoNombre, 
                String primerApellido, String segundoApellido, TipoRRHH tipoRRHH, 
                String numCelular, String email, String iniciales, String orcid, 
                String codigoGenero) {
        this.idRecurso = idRecurso;
        this.primerNombre = primerNombre;
        this.segundoNombre = segundoNombre;
        this.primerApellido = primerApellido;
        this.segundoApellido = segundoApellido;
        this.tipoRRHH = tipoRRHH;
        this.numCelular = numCelular;
        this.email = email;
        this.iniciales = iniciales;
        this.orcid = orcid;
        this.codigoGenero = codigoGenero;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getIdRecurso() {
        return idRecurso;
    }
    
    public void setIdRecurso(String idRecurso) {
        this.idRecurso = idRecurso;
    }
    
    public String getPrimerNombre() {
        return primerNombre;
    }
    
    public void setPrimerNombre(String primerNombre) {
        this.primerNombre = primerNombre;
    }
    
    public String getSegundoNombre() {
        return segundoNombre;
    }
    
    public void setSegundoNombre(String segundoNombre) {
        this.segundoNombre = segundoNombre;
    }
    
    public String getPrimerApellido() {
        return primerApellido;
    }
    
    public void setPrimerApellido(String primerApellido) {
        this.primerApellido = primerApellido;
    }
    
    public String getSegundoApellido() {
        return segundoApellido;
    }
    
    public void setSegundoApellido(String segundoApellido) {
        this.segundoApellido = segundoApellido;
    }
    
    // fullname es calculado desde la función MySQL, solo lectura
    public String getFullname() {
        return fullname;
    }
    
    // No se debe establecer fullname directamente, se calcula desde la BD
    @Deprecated
    public void setFullname(String fullname) {
        // Este método se mantiene por compatibilidad pero no hace nada
        // El fullname se calcula automáticamente desde la función MySQL
    }
    
    public TipoRRHH getTipoRRHH() {
        return tipoRRHH;
    }
    
    public void setTipoRRHH(TipoRRHH tipoRRHH) {
        this.tipoRRHH = tipoRRHH;
    }
    
    public String getOrcid() {
        return orcid;
    }
    
    public void setOrcid(String orcid) {
        this.orcid = orcid;
    }
    
    public String getNumCelular() {
        return numCelular;
    }
    
    public void setNumCelular(String numCelular) {
        this.numCelular = numCelular;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getIniciales() {
        return iniciales;
    }
    
    public void setIniciales(String iniciales) {
        this.iniciales = iniciales;
    }
    
    public String getCodigoGenero() {
        return codigoGenero;
    }
    
    public void setCodigoGenero(String codigoGenero) {
        this.codigoGenero = codigoGenero;
    }
    
    @JsonIgnore
    public List<ParticipacionProducto> getParticipaciones() {
        return participaciones;
    }
    
    public void setParticipaciones(List<ParticipacionProducto> participaciones) {
        this.participaciones = participaciones;
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
}