package com.sisgic.dto;

/**
 * Datos básicos de la publicación en el preview
 */
public class PublicationPreviewDataDTO {
    
    private String title;
    private String displayName;
    private String publicationDate;
    private Integer publicationYear;
    private String doi;
    private String volume;
    private String firstPage;
    private String lastPage;
    private String openAlexUrl;
    
    // Constructors
    public PublicationPreviewDataDTO() {}
    
    // Getters and Setters
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public String getPublicationDate() {
        return publicationDate;
    }
    
    public void setPublicationDate(String publicationDate) {
        this.publicationDate = publicationDate;
    }
    
    public Integer getPublicationYear() {
        return publicationYear;
    }
    
    public void setPublicationYear(Integer publicationYear) {
        this.publicationYear = publicationYear;
    }
    
    public String getDoi() {
        return doi;
    }
    
    public void setDoi(String doi) {
        this.doi = doi;
    }
    
    public String getVolume() {
        return volume;
    }
    
    public void setVolume(String volume) {
        this.volume = volume;
    }
    
    public String getFirstPage() {
        return firstPage;
    }
    
    public void setFirstPage(String firstPage) {
        this.firstPage = firstPage;
    }
    
    public String getLastPage() {
        return lastPage;
    }
    
    public void setLastPage(String lastPage) {
        this.lastPage = lastPage;
    }
    
    public String getOpenAlexUrl() {
        return openAlexUrl;
    }
    
    public void setOpenAlexUrl(String openAlexUrl) {
        this.openAlexUrl = openAlexUrl;
    }
}







