package com.sisgic.dto;

/**
 * DTO para representar el matching de un journal desde OpenAlex
 */
public class JournalMatchDTO {
    
    private Long id;                        // ID del journal si se encuentra en BD
    private String issn;                    // ISSN de OpenAlex
    private String name;                     // Nombre del journal de OpenAlex
    private Boolean foundInDatabase;         // true si existe en BD, false si no
    
    // Constructors
    public JournalMatchDTO() {}
    
    public JournalMatchDTO(String issn, String name, Boolean foundInDatabase) {
        this.issn = issn;
        this.name = name;
        this.foundInDatabase = foundInDatabase;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getIssn() {
        return issn;
    }
    
    public void setIssn(String issn) {
        this.issn = issn;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Boolean getFoundInDatabase() {
        return foundInDatabase;
    }
    
    public void setFoundInDatabase(Boolean foundInDatabase) {
        this.foundInDatabase = foundInDatabase;
    }
}







