package com.sisgic.dto;

import java.util.List;

/**
 * Decisión del usuario sobre un autor y sus afiliaciones
 */
public class AuthorImportDecisionDTO {
    
    private String openAlexId;  // ID del autor en OpenAlex (para referencia)
    private String name;         // Nombre del autor
    private String orcid;        // ORCID del autor
    private Integer order;       // Orden del autor
    private Boolean isCorresponding;  // Si es corresponding author
    private String authorPosition;    // "first", "last", "middle"
    
    // Decisión sobre el RRHH
    private String action;       // "link" | "create" | "matched" (si ya está matched, no requiere acción)
    private Long rrhhId;         // ID del RRHH si action = "link"
    private Long tipoParticipacionId;  // ID del tipo de participación
    
    // Afiliaciones con sus decisiones
    private List<AffiliationImportDecisionDTO> affiliations;
    
    // Constructors
    public AuthorImportDecisionDTO() {}
    
    // Getters and Setters
    public String getOpenAlexId() {
        return openAlexId;
    }
    
    public void setOpenAlexId(String openAlexId) {
        this.openAlexId = openAlexId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getOrcid() {
        return orcid;
    }
    
    public void setOrcid(String orcid) {
        this.orcid = orcid;
    }
    
    public Integer getOrder() {
        return order;
    }
    
    public void setOrder(Integer order) {
        this.order = order;
    }
    
    public Boolean getIsCorresponding() {
        return isCorresponding;
    }
    
    public void setIsCorresponding(Boolean isCorresponding) {
        this.isCorresponding = isCorresponding;
    }
    
    public String getAuthorPosition() {
        return authorPosition;
    }
    
    public void setAuthorPosition(String authorPosition) {
        this.authorPosition = authorPosition;
    }
    
    public String getAction() {
        return action;
    }
    
    public void setAction(String action) {
        this.action = action;
    }
    
    public Long getRrhhId() {
        return rrhhId;
    }
    
    public void setRrhhId(Long rrhhId) {
        this.rrhhId = rrhhId;
    }
    
    public Long getTipoParticipacionId() {
        return tipoParticipacionId;
    }
    
    public void setTipoParticipacionId(Long tipoParticipacionId) {
        this.tipoParticipacionId = tipoParticipacionId;
    }
    
    public List<AffiliationImportDecisionDTO> getAffiliations() {
        return affiliations;
    }
    
    public void setAffiliations(List<AffiliationImportDecisionDTO> affiliations) {
        this.affiliations = affiliations;
    }
}







