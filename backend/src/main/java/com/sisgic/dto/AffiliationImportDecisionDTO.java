package com.sisgic.dto;

/**
 * Decisión del usuario sobre una afiliación
 */
public class AffiliationImportDecisionDTO {
    
    private String name;  // Nombre de la institución desde OpenAlex
    private String rawAffiliationString;  // String crudo de la afiliación
    
    private String action;  // "link" | "create" | "matched" (si ya está matched, no requiere acción)
    private Long institutionId;  // ID de la institución si action = "link"
    private String texto;  // Texto adicional de la afiliación
    
    // Constructors
    public AffiliationImportDecisionDTO() {}
    
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
    
    public String getAction() {
        return action;
    }
    
    public void setAction(String action) {
        this.action = action;
    }
    
    public Long getInstitutionId() {
        return institutionId;
    }
    
    public void setInstitutionId(Long institutionId) {
        this.institutionId = institutionId;
    }
    
    public String getTexto() {
        return texto;
    }
    
    public void setTexto(String texto) {
        this.texto = texto;
    }
}







