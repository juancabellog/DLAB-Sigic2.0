package com.sisgic.dto;

import java.time.LocalDate;

/**
 * DTO para proyectos con conteo de productos asociados
 * Solo lectura - datos pre-procesados desde la vista
 */
public class ProyectoDTO {
    
    private String codigo;
    private String idDescripcion;
    private String descripcion;
    private LocalDate fechaInicio;
    private LocalDate fechaTermino;
    private String codigoExterno;
    private String tipoFinanciamiento;
    private String realizaCon;
    private String createdAt;
    private String updatedAt;
    private Long totalProductos;
    
    // Constructors
    public ProyectoDTO() {}
    
    public ProyectoDTO(String codigo, String idDescripcion, String descripcion, LocalDate fechaInicio, 
                      LocalDate fechaTermino, String codigoExterno, String tipoFinanciamiento, 
                      String realizaCon, String createdAt, String updatedAt, Long totalProductos) {
        this.codigo = codigo;
        this.idDescripcion = idDescripcion;
        this.descripcion = descripcion;
        this.fechaInicio = fechaInicio;
        this.fechaTermino = fechaTermino;
        this.codigoExterno = codigoExterno;
        this.tipoFinanciamiento = tipoFinanciamiento;
        this.realizaCon = realizaCon;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.totalProductos = totalProductos;
    }
    
    // Getters and Setters
    public String getCodigo() {
        return codigo;
    }
    
    public void setCodigo(String codigo) {
        this.codigo = codigo;
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
    
    public Long getTotalProductos() {
        return totalProductos;
    }
    
    public void setTotalProductos(Long totalProductos) {
        this.totalProductos = totalProductos;
    }
    
    public String getIdDescripcion() {
        return idDescripcion;
    }
    
    public void setIdDescripcion(String idDescripcion) {
        this.idDescripcion = idDescripcion;
    }
    
    public LocalDate getFechaInicio() {
        return fechaInicio;
    }
    
    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }
    
    public LocalDate getFechaTermino() {
        return fechaTermino;
    }
    
    public void setFechaTermino(LocalDate fechaTermino) {
        this.fechaTermino = fechaTermino;
    }
    
    public String getCodigoExterno() {
        return codigoExterno;
    }
    
    public void setCodigoExterno(String codigoExterno) {
        this.codigoExterno = codigoExterno;
    }
    
    public String getTipoFinanciamiento() {
        return tipoFinanciamiento;
    }
    
    public void setTipoFinanciamiento(String tipoFinanciamiento) {
        this.tipoFinanciamiento = tipoFinanciamiento;
    }
    
    public String getRealizaCon() {
        return realizaCon;
    }
    
    public void setRealizaCon(String realizaCon) {
        this.realizaCon = realizaCon;
    }
    
    @Override
    public String toString() {
        return "ProyectoDTO{" +
                "codigo='" + codigo + '\'' +
                ", idDescripcion='" + idDescripcion + '\'' +
                ", descripcion='" + descripcion + '\'' +
                ", fechaInicio=" + fechaInicio +
                ", fechaTermino=" + fechaTermino +
                ", codigoExterno='" + codigoExterno + '\'' +
                ", tipoFinanciamiento='" + tipoFinanciamiento + '\'' +
                ", realizaCon='" + realizaCon + '\'' +
                ", createdAt='" + createdAt + '\'' +
                ", updatedAt='" + updatedAt + '\'' +
                ", totalProductos=" + totalProductos +
                '}';
    }
}
