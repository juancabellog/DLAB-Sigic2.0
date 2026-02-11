package com.sisgic.dto;

import java.util.List;

/**
 * DTO para el preview de una publicación cargada desde OpenAlex usando DOI
 * Incluye estados de matching para que el frontend pueda mostrar decisiones pendientes
 */
public class PublicationPreviewDTO {
    
    // Datos básicos de la publicación
    private PublicationPreviewDataDTO publication;
    
    // Journal con estado de matching
    private JournalPreviewDTO journal;
    
    // Autores con estados de matching
    private List<AuthorPreviewDTO> authors;
    
    // Constructors
    public PublicationPreviewDTO() {}
    
    // Getters and Setters
    public PublicationPreviewDataDTO getPublication() {
        return publication;
    }
    
    public void setPublication(PublicationPreviewDataDTO publication) {
        this.publication = publication;
    }
    
    public JournalPreviewDTO getJournal() {
        return journal;
    }
    
    public void setJournal(JournalPreviewDTO journal) {
        this.journal = journal;
    }
    
    public List<AuthorPreviewDTO> getAuthors() {
        return authors;
    }
    
    public void setAuthors(List<AuthorPreviewDTO> authors) {
        this.authors = authors;
    }
}







