package com.sisgic.dto;

public class CreateTipoProductoDTO {
    private String descripcion; // Descripción que ingresa el usuario
    
    // Constructors
    public CreateTipoProductoDTO() {}
    
    public CreateTipoProductoDTO(String descripcion) {
        this.descripcion = descripcion;
    }
    
    // Getters and Setters
    public String getDescripcion() {
        return descripcion;
    }
    
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}











