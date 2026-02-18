package com.sisgic.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "reportes")
public class Reporte {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "descripcion", nullable = false, length = 100)
    private String descripcion;
    
    @Lob
    @Column(name = "excelFile")
    private byte[] excelFile;
    
    @Column(name = "titleCell", nullable = false, length = 5)
    private String titleCell;
    
    @Column(name = "tableCell", nullable = false, length = 5)
    private String tableCell;
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
    
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
    
    public byte[] getExcelFile() {
        return excelFile;
    }
    
    public void setExcelFile(byte[] excelFile) {
        this.excelFile = excelFile;
    }
    
    public String getTitleCell() {
        return titleCell;
    }
    
    public void setTitleCell(String titleCell) {
        this.titleCell = titleCell;
    }
    
    public String getTableCell() {
        return tableCell;
    }
    
    public void setTableCell(String tableCell) {
        this.tableCell = tableCell;
    }
}
