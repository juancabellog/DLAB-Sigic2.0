package com.sisgic.dto;

/**
 * DTO para Tipo de RRHH
 */
public class TipoRRHHDTO {
    
    private Long id;
    private String idDescripcion;
    private String descripcion; // Traducida desde textos
    private String createdAt;
    private String updatedAt;
    
    // Constructors
    public TipoRRHHDTO() {}
    
    public TipoRRHHDTO(Long id, String idDescripcion, String descripcion, 
                       String createdAt, String updatedAt) {
        this.id = id;
        this.idDescripcion = idDescripcion;
        this.descripcion = descripcion;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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











