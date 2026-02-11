package com.sisgic.entity;

import java.io.Serializable;
import java.util.Objects;

public class ParticipacionProductoId implements Serializable {
    
    private Long rrhhId;
    private Long productoId;
    private Long id;
    
    // Constructors
    public ParticipacionProductoId() {}
    
    public ParticipacionProductoId(Long rrhhId, Long productoId, Long id) {
        this.rrhhId = rrhhId;
        this.productoId = productoId;
        this.id = id;
    }
    
    // Constructor legacy para compatibilidad (solo usa idRRHH e idProducto)
    public ParticipacionProductoId(Long rrhhId, Long productoId) {
        this.rrhhId = rrhhId;
        this.productoId = productoId;
        this.id = null; // Se debe establecer después
    }
    
    // Getters and Setters
    public Long getRrhhId() {
        return rrhhId;
    }
    
    public void setRrhhId(Long rrhhId) {
        this.rrhhId = rrhhId;
    }
    
    public Long getProductoId() {
        return productoId;
    }
    
    public void setProductoId(Long productoId) {
        this.productoId = productoId;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParticipacionProductoId that = (ParticipacionProductoId) o;
        return Objects.equals(rrhhId, that.rrhhId) && 
               Objects.equals(productoId, that.productoId) &&
               Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(rrhhId, productoId, id);
    }
}





