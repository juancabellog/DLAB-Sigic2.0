package com.sisgic.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Formula;

@Entity
@Table(name = "becariospostdoctorales")
@PrimaryKeyJoinColumn(name = "id")
public class BecariosPostdoctorales extends ProductoCientifico {
    
    @ManyToOne
    @JoinColumn(name = "idInstitucion")
    private Institucion institucion;
    
    @Lob
    @Column(name = "fundingSource", nullable = false)
    private String fundingSource; // JSON string o lista separada por comas
    
    @ManyToOne
    @JoinColumn(name = "idTipoSector")
    private TipoSector tipoSector;
    
    @Lob
    @Column(name = "resources")
    private String resources; // JSON string o lista separada por comas
    
    @Formula("(SELECT f_getParticipantByRol(id, 19))")
    private String postdoctoralFellowName; // Campo calculado: nombre del becario postdoctoral (rol 19)
    
    // Constructors
    public BecariosPostdoctorales() {}
    
    // Getters and Setters
    public Institucion getInstitucion() {
        return institucion;
    }
    
    public void setInstitucion(Institucion institucion) {
        this.institucion = institucion;
    }
    
    public String getFundingSource() {
        return fundingSource;
    }
    
    public void setFundingSource(String fundingSource) {
        this.fundingSource = fundingSource;
    }
    
    public TipoSector getTipoSector() {
        return tipoSector;
    }
    
    public void setTipoSector(TipoSector tipoSector) {
        this.tipoSector = tipoSector;
    }
    
    public String getResources() {
        return resources;
    }
    
    public void setResources(String resources) {
        this.resources = resources;
    }
    
    public String getPostdoctoralFellowName() {
        return postdoctoralFellowName;
    }
    
    public void setPostdoctoralFellowName(String postdoctoralFellowName) {
        this.postdoctoralFellowName = postdoctoralFellowName;
    }
}










