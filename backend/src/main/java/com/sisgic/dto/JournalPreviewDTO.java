package com.sisgic.dto;

import java.util.List;

/**
 * Journal en el preview con estado de matching
 */
public class JournalPreviewDTO {
    
    private String name;
    private String issn;
    private String status;  // "matched" | "new" | "review"
    private Long matchId;   // ID del journal si status = "matched"
    private List<String> candidates;  // Si status = "review"
    
    // Constructors
    public JournalPreviewDTO() {}
    
    // Getters and Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getIssn() {
        return issn;
    }
    
    public void setIssn(String issn) {
        this.issn = issn;
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







