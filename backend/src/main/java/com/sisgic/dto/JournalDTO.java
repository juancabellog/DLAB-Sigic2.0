package com.sisgic.dto;

import java.time.LocalDateTime;

public class JournalDTO {
    private Long id;
    private String idDescripcion;
    private String descripcion;
    private String abbreviation;
    private String issn;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public JournalDTO() {}

    // Constructor para el query del repositorio
    public JournalDTO(Long id, String idDescripcion, String descripcion, String abbreviation, String issn, 
                     LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.idDescripcion = idDescripcion;
        this.descripcion = descripcion;
        this.abbreviation = abbreviation;
        this.issn = issn;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public String getAbbreviation() {
        return abbreviation;
    }

    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public String getIssn() {
        return issn;
    }

    public void setIssn(String issn) {
        this.issn = issn;
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











