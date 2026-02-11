package com.sisgic.entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * Clave primaria compuesta para la entidad Afiliacion
 */
public class AfiliacionId implements Serializable {
    
    private Long idRRHH;
    private Long idProducto;
    private Long idRRHHProducto;
    private Long id;
    
    // Constructors
    public AfiliacionId() {}
    
    public AfiliacionId(Long idRRHH, Long idProducto, Long idRRHHProducto, Long id) {
        this.idRRHH = idRRHH;
        this.idProducto = idProducto;
        this.idRRHHProducto = idRRHHProducto;
        this.id = id;
    }
    
    // Getters and Setters
    public Long getIdRRHH() {
        return idRRHH;
    }
    
    public void setIdRRHH(Long idRRHH) {
        this.idRRHH = idRRHH;
    }
    
    public Long getIdProducto() {
        return idProducto;
    }
    
    public void setIdProducto(Long idProducto) {
        this.idProducto = idProducto;
    }
    
    public Long getIdRRHHProducto() {
        return idRRHHProducto;
    }
    
    public void setIdRRHHProducto(Long idRRHHProducto) {
        this.idRRHHProducto = idRRHHProducto;
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
        AfiliacionId that = (AfiliacionId) o;
        return Objects.equals(idRRHH, that.idRRHH) &&
               Objects.equals(idProducto, that.idProducto) &&
               Objects.equals(idRRHHProducto, that.idRRHHProducto) &&
               Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(idRRHH, idProducto, idRRHHProducto, id);
    }
}









