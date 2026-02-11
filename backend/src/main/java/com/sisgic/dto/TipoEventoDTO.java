package com.sisgic.dto;

/**
 * DTO para TipoEvento
 */
public class TipoEventoDTO {
    private Long id;
    private String idDescripcion;
    private String descripcion;
    private String createdAt;
    private String updatedAt;
    
    public TipoEventoDTO() {}
    
    public TipoEventoDTO(Long id, String idDescripcion, String descripcion) {
        this.id = id;
        this.idDescripcion = idDescripcion;
        this.descripcion = descripcion;
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
    
    public String getDescripcion() {
        return descripcion;
    }
    
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
    
    public String getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}










