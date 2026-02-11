package com.sisgic.dto;

import java.time.LocalDateTime;

public class TipoColaboracionDTO {
    private Long id;
    private String idDescripcion;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructors
    public TipoColaboracionDTO() {}
    
    public TipoColaboracionDTO(Long id, String idDescripcion) {
        this.id = id;
        this.idDescripcion = idDescripcion;
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










