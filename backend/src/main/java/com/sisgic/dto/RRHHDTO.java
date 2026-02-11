package com.sisgic.dto;

/**
 * DTO para RRHH (Investigadores)
 */
public class RRHHDTO {
    
    private Long id;
    private String idRecurso; // RUT Chile
    private String fullname;
    private Long idTipoRRHH;
    private TipoRRHHDTO tipoRRHH;
    private String numCelular;
    private String email;
    private String iniciales;
    private String orcid;
    private String codigoGenero; // M o F
    private String createdAt;
    private String updatedAt;
    
    // Constructors
    public RRHHDTO() {}
    
    public RRHHDTO(Long id, String idRecurso, String fullname, Long idTipoRRHH, 
                   TipoRRHHDTO tipoRRHH, String numCelular, String email, 
                   String iniciales, String orcid, String codigoGenero, 
                   String createdAt, String updatedAt) {
        this.id = id;
        this.idRecurso = idRecurso;
        this.fullname = fullname;
        this.idTipoRRHH = idTipoRRHH;
        this.tipoRRHH = tipoRRHH;
        this.numCelular = numCelular;
        this.email = email;
        this.iniciales = iniciales;
        this.orcid = orcid;
        this.codigoGenero = codigoGenero;
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
    
    public String getIdRecurso() {
        return idRecurso;
    }
    
    public void setIdRecurso(String idRecurso) {
        this.idRecurso = idRecurso;
    }
    
    public String getFullname() {
        return fullname;
    }
    
    public void setFullname(String fullname) {
        this.fullname = fullname;
    }
    
    public Long getIdTipoRRHH() {
        return idTipoRRHH;
    }
    
    public void setIdTipoRRHH(Long idTipoRRHH) {
        this.idTipoRRHH = idTipoRRHH;
    }
    
    public TipoRRHHDTO getTipoRRHH() {
        return tipoRRHH;
    }
    
    public void setTipoRRHH(TipoRRHHDTO tipoRRHH) {
        this.tipoRRHH = tipoRRHH;
    }
    
    public String getNumCelular() {
        return numCelular;
    }
    
    public void setNumCelular(String numCelular) {
        this.numCelular = numCelular;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getIniciales() {
        return iniciales;
    }
    
    public void setIniciales(String iniciales) {
        this.iniciales = iniciales;
    }
    
    public String getOrcid() {
        return orcid;
    }
    
    public void setOrcid(String orcid) {
        this.orcid = orcid;
    }
    
    public String getCodigoGenero() {
        return codigoGenero;
    }
    
    public void setCodigoGenero(String codigoGenero) {
        this.codigoGenero = codigoGenero;
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
}











