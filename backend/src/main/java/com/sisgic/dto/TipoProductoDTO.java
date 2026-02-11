package com.sisgic.dto;

public class TipoProductoDTO {
    
    private Long id;
    private String idDescripcion; // Referencia a textos.codigoTexto
    private String descripcion; // Valor traducido desde textos
    
    // Constructors
    public TipoProductoDTO() {}
    
    public TipoProductoDTO(Long id, String idDescripcion, String descripcion) {
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
}
