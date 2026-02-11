package com.sisgic.dto;

/**
 * DTO para Estado de Producto
 */
public class EstadoProductoDTO {
    
    private Long id;
    private String codigoDescripcion;
    private String createdAt;
    private String updatedAt;
    
    // Constructors
    public EstadoProductoDTO() {}
    
    public EstadoProductoDTO(Long id, String codigoDescripcion, String createdAt, String updatedAt) {
        this.id = id;
        this.codigoDescripcion = codigoDescripcion;
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
    
    public String getCodigoDescripcion() {
        return codigoDescripcion;
    }
    
    public void setCodigoDescripcion(String codigoDescripcion) {
        this.codigoDescripcion = codigoDescripcion;
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










