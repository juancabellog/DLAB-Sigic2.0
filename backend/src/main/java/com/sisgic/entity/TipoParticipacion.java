package com.sisgic.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "v_tipo_participacion")
@EntityListeners(AuditingEntityListener.class)
public class TipoParticipacion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "idDescripcion", nullable = false)
    private String idDescripcion; // Referencia a textos.codigoTexto
    
    @Column(name = "idTipoProducto")
    private Long idTipoProducto; // Referencia a tipoproducto.id
    
    @Column(name = "descripcion")
    private String descripcion; // Valor traducido desde textos
    
    @Column(name = "tipo_producto_nombre")
    private String tipoProductoNombre; // Nombre del tipo de producto
    
    @Transient
    private String aplicableProductos; // Productos a los que aplica este tipo de participación (ALL, PUBLICATIONS, etc.) - Calculado basado en idTipoProducto
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public TipoParticipacion() {}
    
    public TipoParticipacion(String idDescripcion, Long idTipoProducto, String descripcion, String tipoProductoNombre) {
        this.idDescripcion = idDescripcion;
        this.idTipoProducto = idTipoProducto;
        this.descripcion = descripcion;
        this.tipoProductoNombre = tipoProductoNombre;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getIdDescripcion() {
        return idDescripcion;
    }
    
    public void setIdDescripcion(String idDescripcion) {
        this.idDescripcion = idDescripcion;
    }
    
    public Long getIdTipoProducto() {
        return idTipoProducto;
    }
    
    public void setIdTipoProducto(Long idTipoProducto) {
        this.idTipoProducto = idTipoProducto;
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
    
    public String getDescripcion() {
        return descripcion;
    }
    
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
    
    public String getTipoProductoNombre() {
        return tipoProductoNombre;
    }
    
    public void setTipoProductoNombre(String tipoProductoNombre) {
        this.tipoProductoNombre = tipoProductoNombre;
    }
    
    public String getAplicableProductos() {
        return aplicableProductos;
    }
    
    public void setAplicableProductos(String aplicableProductos) {
        this.aplicableProductos = aplicableProductos;
    }
}



