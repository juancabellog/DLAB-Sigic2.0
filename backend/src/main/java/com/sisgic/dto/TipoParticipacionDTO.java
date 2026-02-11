package com.sisgic.dto;

public class TipoParticipacionDTO {
    private Long id;
    private String idDescripcion;
    private Long idTipoProducto;
    private String descripcion; // Valor traducido desde textos
    private String tipoProductoNombre; // Nombre del tipo de producto
    private String aplicableProductos; // Productos a los que aplica este tipo de participación (ALL, PUBLICATIONS, etc.)
    
    // Constructors
    public TipoParticipacionDTO() {}
    
    public TipoParticipacionDTO(Long id, String idDescripcion, Long idTipoProducto, String descripcion, String tipoProductoNombre) {
        this.id = id;
        this.idDescripcion = idDescripcion;
        this.idTipoProducto = idTipoProducto;
        this.descripcion = descripcion;
        this.tipoProductoNombre = tipoProductoNombre;
    }
    
    public TipoParticipacionDTO(Long id, String idDescripcion, Long idTipoProducto, String descripcion, String tipoProductoNombre, String aplicableProductos) {
        this.id = id;
        this.idDescripcion = idDescripcion;
        this.idTipoProducto = idTipoProducto;
        this.descripcion = descripcion;
        this.tipoProductoNombre = tipoProductoNombre;
        this.aplicableProductos = aplicableProductos;
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
    
    public Long getIdTipoProducto() {
        return idTipoProducto;
    }
    
    public void setIdTipoProducto(Long idTipoProducto) {
        this.idTipoProducto = idTipoProducto;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
    
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
    
    public String getTipoProductoNombre() {
        return tipoProductoNombre;
    }
    
    public void setTipoProductoNombre(String tipoProductoNombre) {
        this.tipoProductoNombre = tipoProductoNombre;
    }
    
    public String getAplicableProductos() {
        return aplicableProductos;
    }
    
    public void setAplicableProductos(String aplicableProductos) {
        this.aplicableProductos = aplicableProductos;
    }
}