package com.sisgic.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "publicacion")
@PrimaryKeyJoinColumn(name = "id")
public class Publicacion extends ProductoCientifico {
    
    @ManyToOne
    @JoinColumn(name = "idJournal")
    private Journal journal;
    
    private String volume;
    
    @Column(name = "yearPublished")
    private Integer yearPublished;
    
    private String firstpage;
    private String lastpage;
    
    // CAMPOS JSON - selección múltiple
    @Column(name = "indexs")
    private String indexs; // JSON string
    
    @Column(name = "funding")
    private String funding; // JSON string
    
    private String doi;
    
    @Column(name = "numCitas")
    private Integer numCitas;
    
    @Column(name = "impactFactor", precision = 8, scale = 4)
    private java.math.BigDecimal impactFactor;
    
    @Column(name = "avgImpactFactor", precision = 8, scale = 4)
    private java.math.BigDecimal avgImpactFactor;
    
    // Constructors
    public Publicacion() {}
    
    // Getters and Setters
    public Journal getJournal() {
        return journal;
    }
    
    public void setJournal(Journal journal) {
        this.journal = journal;
    }
    
    public String getVolume() {
        return volume;
    }
    
    public void setVolume(String volume) {
        this.volume = volume;
    }
    
    public Integer getYearPublished() {
        return yearPublished;
    }
    
    public void setYearPublished(Integer yearPublished) {
        this.yearPublished = yearPublished;
    }
    
    public String getFirstpage() {
        return firstpage;
    }
    
    public void setFirstpage(String firstpage) {
        this.firstpage = firstpage;
    }
    
    public String getLastpage() {
        return lastpage;
    }
    
    public void setLastpage(String lastpage) {
        this.lastpage = lastpage;
    }
    
    public String getIndexs() {
        return indexs;
    }
    
    public void setIndexs(String indexs) {
        this.indexs = indexs;
    }
    
    public String getFunding() {
        return funding;
    }
    
    public void setFunding(String funding) {
        this.funding = funding;
    }
    
    public String getDoi() {
        return doi;
    }
    
    public void setDoi(String doi) {
        this.doi = doi;
    }
    
    public Integer getNumCitas() {
        return numCitas;
    }
    
    public void setNumCitas(Integer numCitas) {
        this.numCitas = numCitas;
    }
    
    public java.math.BigDecimal getImpactFactor() {
        return impactFactor;
    }
    
    public void setImpactFactor(java.math.BigDecimal impactFactor) {
        this.impactFactor = impactFactor;
    }
    
    public java.math.BigDecimal getAvgImpactFactor() {
        return avgImpactFactor;
    }
    
    public void setAvgImpactFactor(java.math.BigDecimal avgImpactFactor) {
        this.avgImpactFactor = avgImpactFactor;
    }
}