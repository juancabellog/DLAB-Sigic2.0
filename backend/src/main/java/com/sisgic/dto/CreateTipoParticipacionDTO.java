package com.sisgic.dto;

public class CreateTipoParticipacionDTO {
    private String descripcion; // Descripción que ingresa el usuario
    private Long idTipoProducto; // ID del tipo de producto seleccionado
    
    // Constructors
    public CreateTipoParticipacionDTO() {}
    
    public CreateTipoParticipacionDTO(String descripcion, Long idTipoProducto) {
        this.descripcion = descripcion;
        this.idTipoProducto = idTipoProducto;
    }
    
    // Getters and Setters
    public String getDescripcion() {
        return descripcion;
    }
    
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
    
    public Long getIdTipoProducto() {
        return idTipoProducto;
    }
    
    public void setIdTipoProducto(Long idTipoProducto) {
        this.idTipoProducto = idTipoProducto;
    }
}











