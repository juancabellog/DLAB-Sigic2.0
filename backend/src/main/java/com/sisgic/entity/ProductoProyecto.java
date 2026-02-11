package com.sisgic.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "producto_proyecto")
@EntityListeners(AuditingEntityListener.class)
public class ProductoProyecto {
    
    @EmbeddedId
    private ProductoProyectoId id;
    
    @ManyToOne
    @MapsId("productoId")
    @JoinColumn(name = "idProducto")
    private ProductoCientifico producto;
    
    @ManyToOne
    @MapsId("proyectoCodigo")
    @JoinColumn(name = "codigoProyecto")
    private Proyecto proyecto;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public ProductoProyecto() {}
    
    public ProductoProyecto(ProductoCientifico producto, Proyecto proyecto) {
        this.producto = producto;
        this.proyecto = proyecto;
        this.id = new ProductoProyectoId(producto.getId(), proyecto.getCodigo());
    }
    
    // Getters and Setters
    public ProductoProyectoId getId() {
        return id;
    }
    
    public void setId(ProductoProyectoId id) {
        this.id = id;
    }
    
    public ProductoCientifico getProducto() {
        return producto;
    }
    
    public void setProducto(ProductoCientifico producto) {
        this.producto = producto;
    }
    
    public Proyecto getProyecto() {
        return proyecto;
    }
    
    public void setProyecto(Proyecto proyecto) {
        this.proyecto = proyecto;
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





