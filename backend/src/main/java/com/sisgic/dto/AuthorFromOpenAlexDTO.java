package com.sisgic.dto;

import java.util.List;

/**
 * DTO para representar un autor desde OpenAlex con su información de matching
 */
public class AuthorFromOpenAlexDTO {
    
    private String openAlexId;                // ID del autor en OpenAlex
    private String fullName;                 // Nombre completo del autor (raw_author_name o display_name)
    private String orcid;                    // ORCID si existe
    private Integer order;                   // Orden del autor (1, 2, 3...)
    private Boolean isCorresponding;         // Si es corresponding author (de OpenAlex: is_corresponding)
    private String authorPosition;           // "first", "last", o "middle"
    
    // Matching con nuestra BD
    private ResearcherMatchDTO researcherMatch;  // Resultado del matching con ResearcherMatchingService
    
    // Afiliaciones del autor (desde OpenAlex)
    private List<InstitutionFromOpenAlexDTO> affiliations;
    
    // Constructors
    public AuthorFromOpenAlexDTO() {}
    
    // Getters and Setters
    public String getOpenAlexId() {
        return openAlexId;
    }
    
    public void setOpenAlexId(String openAlexId) {
        this.openAlexId = openAlexId;
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public void setFullName(String fullName) {
        this.fullName = fullName;
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
    
    public ResearcherMatchDTO getResearcherMatch() {
        return researcherMatch;
    }
    
    public void setResearcherMatch(ResearcherMatchDTO researcherMatch) {
        this.researcherMatch = researcherMatch;
    }
    
    public List<InstitutionFromOpenAlexDTO> getAffiliations() {
        return affiliations;
    }
    
    public void setAffiliations(List<InstitutionFromOpenAlexDTO> affiliations) {
        this.affiliations = affiliations;
    }
}

