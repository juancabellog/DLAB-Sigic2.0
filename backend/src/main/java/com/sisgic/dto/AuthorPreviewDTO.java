package com.sisgic.dto;

import java.util.List;

/**
 * Autor en el preview con estado de matching
 */
public class AuthorPreviewDTO {
    
    private String openAlexId;  // ID del autor en OpenAlex (si está disponible)
    private String name;
    private String orcid;
    private Integer order;
    private Boolean isCorresponding;
    private String authorPosition;  // "first", "last", "middle"
    private Long tipoParticipacionId;  // ID del tipo de participación calculado desde authorPosition (1=Author principal, 2=Co-author)
    
    // Estado de matching
    private String status;  // "matched" | "new" | "review"
    private Long matchId;   // ID del RRHH si status = "matched"
    private String matchedName;  // Nombre del RRHH encontrado (de BD)
    private String matchedOrcid; // ORCID del RRHH encontrado (de BD)
    private List<String> candidates;  // Si status = "review"
    
    // Estado de sincronización de ORCID
    private String orcidSyncStatus;  // "ok" | "missing_local" | "conflict"
    private String orcidChangeAction;  // "none" | "add" | "replace" | "unlink"
    private String matchMethod;  // "orcid" | "name" - cómo se hizo el match
    
    // Afiliaciones
    private List<AffiliationPreviewDTO> affiliations;
    
    // Constructors
    public AuthorPreviewDTO() {}
    
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
    
    public Long getTipoParticipacionId() {
        return tipoParticipacionId;
    }
    
    public void setTipoParticipacionId(Long tipoParticipacionId) {
        this.tipoParticipacionId = tipoParticipacionId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public Long getMatchId() {
        return matchId;
    }
    
    public void setMatchId(Long matchId) {
        this.matchId = matchId;
    }
    
    public String getMatchedName() {
        return matchedName;
    }
    
    public void setMatchedName(String matchedName) {
        this.matchedName = matchedName;
    }
    
    public List<String> getCandidates() {
        return candidates;
    }
    
    public void setCandidates(List<String> candidates) {
        this.candidates = candidates;
    }
    
    public List<AffiliationPreviewDTO> getAffiliations() {
        return affiliations;
    }
    
    public void setAffiliations(List<AffiliationPreviewDTO> affiliations) {
        this.affiliations = affiliations;
    }
    
    public String getMatchedOrcid() {
        return matchedOrcid;
    }
    
    public void setMatchedOrcid(String matchedOrcid) {
        this.matchedOrcid = matchedOrcid;
    }
    
    public String getOrcidSyncStatus() {
        return orcidSyncStatus;
    }
    
    public void setOrcidSyncStatus(String orcidSyncStatus) {
        this.orcidSyncStatus = orcidSyncStatus;
    }
    
    public String getOrcidChangeAction() {
        return orcidChangeAction;
    }
    
    public void setOrcidChangeAction(String orcidChangeAction) {
        this.orcidChangeAction = orcidChangeAction;
    }
    
    public String getMatchMethod() {
        return matchMethod;
    }
    
    public void setMatchMethod(String matchMethod) {
        this.matchMethod = matchMethod;
    }
}





