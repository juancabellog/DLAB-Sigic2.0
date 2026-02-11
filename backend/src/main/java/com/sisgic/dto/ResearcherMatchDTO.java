package com.sisgic.dto;

import java.util.List;

/**
 * DTO para representar el resultado del matching de un investigador
 */
public class ResearcherMatchDTO {
    
    private Long matchedId;                  // ID del RRHH si se encontró match
    private String matchedName;             // Nombre del RRHH encontrado
    private String matchedOrcid;            // ORCID del RRHH encontrado (de BD)
    private String matchStatus;             // "UNICA", "MAS_DE_UNA", "SIN_COINCIDENCIAS"
    private List<String> candidates;        // Lista de candidatos si hay múltiples
    private String detail;                   // Detalle del matching (candidatos separados por "/")
    private String orcidSyncStatus;         // "ok" | "missing_local" | "conflict"
    private String matchMethod;             // "orcid" | "name" - cómo se hizo el match
    
    // Constructors
    public ResearcherMatchDTO() {}
    
    public ResearcherMatchDTO(String matchStatus) {
        this.matchStatus = matchStatus;
    }
    
    // Getters and Setters
    public Long getMatchedId() {
        return matchedId;
    }
    
    public void setMatchedId(Long matchedId) {
        this.matchedId = matchedId;
    }
    
    public String getMatchedName() {
        return matchedName;
    }
    
    public void setMatchedName(String matchedName) {
        this.matchedName = matchedName;
    }
    
    public String getMatchStatus() {
        return matchStatus;
    }
    
    public void setMatchStatus(String matchStatus) {
        this.matchStatus = matchStatus;
    }
    
    public List<String> getCandidates() {
        return candidates;
    }
    
    public void setCandidates(List<String> candidates) {
        this.candidates = candidates;
    }
    
    public String getDetail() {
        return detail;
    }
    
    public void setDetail(String detail) {
        this.detail = detail;
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
    
    public String getMatchMethod() {
        return matchMethod;
    }
    
    public void setMatchMethod(String matchMethod) {
        this.matchMethod = matchMethod;
    }
}







