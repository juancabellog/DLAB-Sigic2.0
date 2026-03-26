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
    private String linkPDF; // Link al PDF descargado (formato "PDF:pdfs/uuid.pdf")

    // JSON string with selected index types (same format as Publicacion.indexs)
    private String indexs;

    // JSON array (ids) with selected funding types (same format as Publicacion.funding)
    private String funding;
    
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

    public String getLinkPDF() {
        return linkPDF;
    }

    public void setLinkPDF(String linkPDF) {
        this.linkPDF = linkPDF;
    }

    public String getIndexs() {
        return indexs;
    }

    public void setIndexs(String indexs) {
        this.indexs = indexs;
    }

    public String getFunding() {
        return funding;
    }

    public void setFunding(String funding) {
        this.funding = funding;
    }
}







