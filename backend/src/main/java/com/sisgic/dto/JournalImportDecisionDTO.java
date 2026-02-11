package com.sisgic.dto;

/**
 * Decisión del usuario sobre el journal
 */
public class JournalImportDecisionDTO {
    
    private String action;  // "link" | "create" | "matched" (si ya está matched, no requiere acción)
    private Long journalId;  // ID del journal si action = "link"
    private String name;     // Nombre del journal si action = "create"
    private String issn;     // ISSN del journal
    
    // Constructors
    public JournalImportDecisionDTO() {}
    
    // Getters and Setters
    public String getAction() {
        return action;
    }
    
    public void setAction(String action) {
        this.action = action;
    }
    
    public Long getJournalId() {
        return journalId;
    }
    
    public void setJournalId(Long journalId) {
        this.journalId = journalId;
    }
    
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
}







