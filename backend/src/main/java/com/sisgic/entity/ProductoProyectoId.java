package com.sisgic.entity;

import java.io.Serializable;
import java.util.Objects;

public class ProductoProyectoId implements Serializable {
    
    private Long productoId;
    private String proyectoCodigo;
    
    // Constructors
    public ProductoProyectoId() {}
    
    public ProductoProyectoId(Long productoId, String proyectoCodigo) {
        this.productoId = productoId;
        this.proyectoCodigo = proyectoCodigo;
    }
    
    // Getters and Setters
    public Long getProductoId() {
        return productoId;
    }
    
    public void setProductoId(Long productoId) {
        this.productoId = productoId;
    }
    
    public String getProyectoCodigo() {
        return proyectoCodigo;
    }
    
    public void setProyectoCodigo(String proyectoCodigo) {
        this.proyectoCodigo = proyectoCodigo;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductoProyectoId that = (ProductoProyectoId) o;
        return Objects.equals(productoId, that.productoId) && Objects.equals(proyectoCodigo, that.proyectoCodigo);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(productoId, proyectoCodigo);
    }
}





