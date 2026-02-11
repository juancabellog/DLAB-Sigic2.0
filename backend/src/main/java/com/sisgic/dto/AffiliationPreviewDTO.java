package com.sisgic.dto;

import java.util.List;

/**
 * Afiliación en el preview con estado de matching
 */
public class AffiliationPreviewDTO {
    
    private String name;  // Nombre de la institución desde OpenAlex
    private String rawAffiliationString;  // String crudo de la afiliación
    private String status;  // "matched" | "new" | "review"
    private Long matchId;   // ID de institución si status = "matched"
    private List<String> candidates;  // Si status = "review"
    
    // Constructors
    public AffiliationPreviewDTO() {}
    
    // Getters and Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getRawAffiliationString() {
        return rawAffiliationString;
    }
    
    public void setRawAffiliationString(String rawAffiliationString) {
        this.rawAffiliationString = rawAffiliationString;
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
    
    public List<String> getCandidates() {
        return candidates;
    }
    
    public void setCandidates(List<String> candidates) {
        this.candidates = candidates;
    }
}







