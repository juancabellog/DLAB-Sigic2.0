package com.sisgic.dto;

/**
 * DTO para representar una institución desde OpenAlex con su matching
 */
public class InstitutionFromOpenAlexDTO {
    
    private String name;                     // Nombre de la institución (display_name)
    private String rorId;                    // ROR ID si existe
    private String countryCode;             // Código de país
    private String rawAffiliationString;     // String crudo de la afiliación
    private Long matchedInstitutionId;       // ID de nuestra BD si se encuentra match
    private Boolean foundInDatabase;         // true si existe en BD, false si no
    
    // Constructors
    public InstitutionFromOpenAlexDTO() {}
    
    // Getters and Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getRorId() {
        return rorId;
    }
    
    public void setRorId(String rorId) {
        this.rorId = rorId;
    }
    
    public String getCountryCode() {
        return countryCode;
    }
    
    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }
    
    public String getRawAffiliationString() {
        return rawAffiliationString;
    }
    
    public void setRawAffiliationString(String rawAffiliationString) {
        this.rawAffiliationString = rawAffiliationString;
    }
    
    public Long getMatchedInstitutionId() {
        return matchedInstitutionId;
    }
    
    public void setMatchedInstitutionId(Long matchedInstitutionId) {
        this.matchedInstitutionId = matchedInstitutionId;
    }
    
    public Boolean getFoundInDatabase() {
        return foundInDatabase;
    }
    
    public void setFoundInDatabase(Boolean foundInDatabase) {
        this.foundInDatabase = foundInDatabase;
    }
}







