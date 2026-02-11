package com.sisgic.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "textos")
@EntityListeners(AuditingEntityListener.class)
public class Textos {
    
    @EmbeddedId
    private TextosId id;
    
    @Column(name = "valor", columnDefinition = "TEXT")
    private String valor;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public Textos() {}
    
    public Textos(TextosId id, String valor) {
        this.id = id;
        this.valor = valor;
    }
    
    // Getters and Setters
    public TextosId getId() {
        return id;
    }
    
    public void setId(TextosId id) {
        this.id = id;
    }
    
    public String getValor() {
        return valor;
    }
    
    public void setValor(String valor) {
        this.valor = valor;
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


