package com.sisgic.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "v_proyectos")
@EntityListeners(AuditingEntityListener.class)
public class Proyecto {
    
    @Id
    @Column(name = "codigo")
    private String codigo;
    
    @Column(name = "idDescripcion")
    private String idDescripcion;
    
    @Column(name = "descripcion")
    private String descripcion;
    
    @Column(name = "fechaInicio")
    private LocalDate fechaInicio;
    
    @Column(name = "fechaTermino")
    private LocalDate fechaTermino;
    
    @Column(name = "codigoExterno")
    private String codigoExterno;
    
    @Column(name = "tipoFinanciamiento")
    private String tipoFinanciamiento;
    
    @Column(name = "realizaCon")
    private String realizaCon;
    
    @Column(name = "total_productos")
    private Long totalProductos;
    
    @OneToMany(mappedBy = "proyecto", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ProductoProyecto> productos = new ArrayList<>();
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public Proyecto() {}
    
    public Proyecto(String codigo, String idDescripcion, String descripcion, LocalDate fechaInicio, 
                   LocalDate fechaTermino, String codigoExterno, String tipoFinanciamiento, 
                   String realizaCon, Long totalProductos) {
        this.codigo = codigo;
        this.idDescripcion = idDescripcion;
        this.descripcion = descripcion;
        this.fechaInicio = fechaInicio;
        this.fechaTermino = fechaTermino;
        this.codigoExterno = codigoExterno;
        this.tipoFinanciamiento = tipoFinanciamiento;
        this.realizaCon = realizaCon;
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
    
    @JsonIgnore
    public List<ProductoProyecto> getProductos() {
        return productos;
    }
    
    public void setProductos(List<ProductoProyecto> productos) {
        this.productos = productos;
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
}



