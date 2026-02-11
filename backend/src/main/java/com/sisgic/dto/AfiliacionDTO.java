package com.sisgic.dto;

/**
 * DTO para Afiliacion de un investigador en una publicación
 */
public class AfiliacionDTO {
    
    private Long idRRHH;
    private Long idProducto;
    private Long idRRHHProducto;
    private Long id;
    private Long idInstitucion;
    private String texto;
    
    // Campos adicionales para mostrar en el frontend
    private String nombreInstitucion; // Para mostrar el nombre de la institución
    
    // Constructors
    public AfiliacionDTO() {}
    
    public AfiliacionDTO(Long idRRHH, Long idProducto, Long idRRHHProducto, Long id, 
                         Long idInstitucion, String texto) {
        this.idRRHH = idRRHH;
        this.idProducto = idProducto;
        this.idRRHHProducto = idRRHHProducto;
        this.id = id;
        this.idInstitucion = idInstitucion;
        this.texto = texto;
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
    
    public Long getIdInstitucion() {
        return idInstitucion;
    }
    
    public void setIdInstitucion(Long idInstitucion) {
        this.idInstitucion = idInstitucion;
    }
    
    public String getTexto() {
        return texto;
    }
    
    public void setTexto(String texto) {
        this.texto = texto;
    }
    
    public String getNombreInstitucion() {
        return nombreInstitucion;
    }
    
    public void setNombreInstitucion(String nombreInstitucion) {
        this.nombreInstitucion = nombreInstitucion;
    }
}









