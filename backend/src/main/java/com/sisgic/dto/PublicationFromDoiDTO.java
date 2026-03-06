package com.sisgic.dto;

import java.util.List;

/**
 * DTO para representar una publicación cargada desde OpenAlex usando DOI
 * NO contiene IDs de base de datos ya que no se persiste
 */
public class PublicationFromDoiDTO {
    
    // Datos básicos de la publicación
    private String title;                    // De OpenAlex: title
    private String displayName;             // De OpenAlex: display_name
    private String publicationDate;         // De OpenAlex: publication_date (YYYY-MM-DD)
    private Integer publicationYear;        // De OpenAlex: publication_year
    private String doi;                     // DOI ingresado por el usuario
    
    // Datos del Journal
    private JournalMatchDTO journalMatch;   // Información del journal encontrado o sugerido
    
    // Datos bibliográficos
    private String volume;                  // De OpenAlex: biblio.volume
    private String firstPage;               // De OpenAlex: biblio.first_page
    private String lastPage;                // De OpenAlex: biblio.last_page
    
    // Autores y afiliaciones (sin persistir)
    private List<AuthorFromOpenAlexDTO> authors;  // Array de autores con sus datos
    
    // Metadata adicional
    private String openAlexUrl;             // URL de OpenAlex para referencia
    private String linkPDF;                 // Link al PDF descargado y guardado (formato "PDF:pdfs/uuid.pdf")
    
    // Constructors
    public PublicationFromDoiDTO() {}
    
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
    
    public JournalMatchDTO getJournalMatch() {
        return journalMatch;
    }
    
    public void setJournalMatch(JournalMatchDTO journalMatch) {
        this.journalMatch = journalMatch;
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
    
    public List<AuthorFromOpenAlexDTO> getAuthors() {
        return authors;
    }
    
    public void setAuthors(List<AuthorFromOpenAlexDTO> authors) {
        this.authors = authors;
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
}

