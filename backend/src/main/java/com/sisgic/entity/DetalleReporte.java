package com.sisgic.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "DetalleReportes")
public class DetalleReporte {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "idReporte", nullable = false)
    private Long idReporte;
    
    @Lob
    @Column(name = "sqlQuery")
    private String sqlQuery;
    
    @Column(name = "cell", length = 5)
    private String cell;
    
    @Column(name = "descripcion", nullable = false, length = 500)
    private String descripcion;
    
    @Column(name = "rowmode", nullable = false, length = 1)
    private String rowmode; // '1' = true, '0' = false
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getIdReporte() {
        return idReporte;
    }
    
    public void setIdReporte(Long idReporte) {
        this.idReporte = idReporte;
    }
    
    public String getSqlQuery() {
        return sqlQuery;
    }
    
    public void setSqlQuery(String sqlQuery) {
        this.sqlQuery = sqlQuery;
    }
    
    public String getCell() {
        return cell;
    }
    
    public void setCell(String cell) {
        this.cell = cell;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
    
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
    
    public String getRowmode() {
        return rowmode;
    }
    
    public void setRowmode(String rowmode) {
        this.rowmode = rowmode;
    }
    
    // Helper method to convert rowmode to boolean
    public Boolean getRowMode() {
        return "1".equals(rowmode);
    }
    
    public void setRowMode(Boolean rowMode) {
        this.rowmode = rowMode ? "1" : "0";
    }
}
