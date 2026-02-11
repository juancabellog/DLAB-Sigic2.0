package com.sisgic.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entidad que representa las afiliaciones de un científico (RRHH) a instituciones
 * cuando participa en un producto científico.
 * 
 * Una afiliación corresponde a las instituciones que el científico declara
 * que lo representan en una publicación/producto específico.
 */
@Entity
@Table(name = "afiliacion")
@EntityListeners(AuditingEntityListener.class)
public class Afiliacion {
    
    @EmbeddedId
    private AfiliacionId id;
    
    /**
     * Referencia a la participación (rrhh_producto)
     * La clave foránea es: (idRRHH, idProducto, idRRHHProducto)
     * donde idRRHHProducto referencia a rrhh_producto.id
     * 
     * La clave primaria de rrhh_producto es: (idRRHH, idProducto, id)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "idRRHH", referencedColumnName = "idRRHH", insertable = false, updatable = false),
        @JoinColumn(name = "idProducto", referencedColumnName = "idProducto", insertable = false, updatable = false),
        @JoinColumn(name = "idRRHHProducto", referencedColumnName = "id", insertable = false, updatable = false)
    })
    @JsonIgnore
    private ParticipacionProducto participacionProducto;
    
    /**
     * Institución a la que pertenece la afiliación
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idInstitucion")
    private Institucion institucion;
    
    /**
     * Texto descriptivo de la afiliación
     */
    @Column(name = "texto", columnDefinition = "TEXT")
    private String texto;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public Afiliacion() {}
    
    public Afiliacion(AfiliacionId id, Institucion institucion, String texto) {
        this.id = id;
        this.institucion = institucion;
        this.texto = texto;
    }
    
    // Getters and Setters
    public AfiliacionId getId() {
        return id;
    }
    
    public void setId(AfiliacionId id) {
        this.id = id;
    }
    
    public ParticipacionProducto getParticipacionProducto() {
        return participacionProducto;
    }
    
    public void setParticipacionProducto(ParticipacionProducto participacionProducto) {
        this.participacionProducto = participacionProducto;
    }
    
    public Institucion getInstitucion() {
        return institucion;
    }
    
    public void setInstitucion(Institucion institucion) {
        this.institucion = institucion;
    }
    
    public String getTexto() {
        return texto;
    }
    
    public void setTexto(String texto) {
        this.texto = texto;
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

