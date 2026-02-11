package com.sisgic.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "rrhh_producto")
@EntityListeners(AuditingEntityListener.class)
public class ParticipacionProducto {
    
    @EmbeddedId
    private ParticipacionProductoId id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("rrhhId")
    @JoinColumn(name = "idRRHH")
    @JsonIgnore
    private RRHH rrhh;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("productoId")
    @JoinColumn(name = "idProducto")
    @JsonIgnore
    private ProductoCientifico producto;
    
    // El campo id es parte de la clave primaria compuesta (idRRHH, idProducto, id)
    // Se mapea automáticamente a través de @EmbeddedId
    // NOTA: El campo id debe establecerse explícitamente al crear una nueva participación
    // ya que no es auto-generado
    
    private Integer orden;
    
    @ManyToOne
    @JoinColumn(name = "idTipoParticipacion")
    private TipoParticipacion tipoParticipacion;
    
    // PROPIEDAD ESPECÍFICA PARA PUBLICACIONES
    @Column(name = "corresponding")
    private Character corresponding; // '1' = true, '0' = false
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public ParticipacionProducto() {}
    
    public ParticipacionProducto(RRHH rrhh, ProductoCientifico producto, TipoParticipacion tipoParticipacion) {
        this.rrhh = rrhh;
        this.producto = producto;
        this.tipoParticipacion = tipoParticipacion;
        this.id = new ParticipacionProductoId(rrhh.getId(), producto.getId());
    }
    
    // Getters and Setters
    public ParticipacionProductoId getId() {
        return id;
    }
    
    public void setId(ParticipacionProductoId id) {
        this.id = id;
    }
    
    public RRHH getRrhh() {
        return rrhh;
    }
    
    public void setRrhh(RRHH rrhh) {
        this.rrhh = rrhh;
    }
    
    public ProductoCientifico getProducto() {
        return producto;
    }
    
    public void setProducto(ProductoCientifico producto) {
        this.producto = producto;
    }
    
    public Integer getOrden() {
        return orden;
    }
    
    public void setOrden(Integer orden) {
        this.orden = orden;
    }
    
    public TipoParticipacion getTipoParticipacion() {
        return tipoParticipacion;
    }
    
    public void setTipoParticipacion(TipoParticipacion tipoParticipacion) {
        this.tipoParticipacion = tipoParticipacion;
    }
    
    public Character getCorresponding() {
        return corresponding;
    }
    
    public void setCorresponding(Character corresponding) {
        this.corresponding = corresponding;
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
    
    // Business methods
    public boolean isCorresponding() {
        return corresponding != null && corresponding == '1';
    }
    
    public void setCorresponding(boolean isCorresponding) {
        this.corresponding = isCorresponding ? '1' : '0';
    }
}