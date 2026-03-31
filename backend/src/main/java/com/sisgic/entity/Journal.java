package com.sisgic.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "journal")
@EntityListeners(AuditingEntityListener.class)
public class Journal {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "iddescripcion")
    private String idDescripcion;
    
    @Column(name = "abbreviation")
    private String abbreviation;
    
    @Column(name = "issn")
    private String issn;

    @Column(name = "is_preprint", length = 1, columnDefinition = "CHAR(1)")
    private Character isPreprint; // S o N
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public Journal() {}
    
    public Journal(String idDescripcion, String abbreviation, String issn) {
        this.idDescripcion = idDescripcion;
        this.abbreviation = abbreviation;
        this.issn = issn;
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

    public Character getIsPreprint() {
        return isPreprint;
    }

    public void setIsPreprint(Character isPreprint) {
        this.isPreprint = isPreprint;
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



