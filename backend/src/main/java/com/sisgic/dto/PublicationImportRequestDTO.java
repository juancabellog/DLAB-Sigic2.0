package com.sisgic.dto;

import java.util.List;

/**
 * DTO para la importación de una publicación desde DOI
 * Contiene los datos de la publicación y las decisiones del usuario sobre autores, instituciones y journal
 */
public class PublicationImportRequestDTO {
    
    // Datos de la publicación
    private PublicationPreviewDataDTO publication;
    
    // Decisión sobre el journal
    private JournalImportDecisionDTO journal;
    
    // Autores con sus decisiones
    private List<AuthorImportDecisionDTO> authors;
    
    // Constructors
    public PublicationImportRequestDTO() {}
    
    // Getters and Setters
    public PublicationPreviewDataDTO getPublication() {
        return publication;
    }
    
    public void setPublication(PublicationPreviewDataDTO publication) {
        this.publication = publication;
    }
    
    public JournalImportDecisionDTO getJournal() {
        return journal;
    }
    
    public void setJournal(JournalImportDecisionDTO journal) {
        this.journal = journal;
    }
    
    public List<AuthorImportDecisionDTO> getAuthors() {
        return authors;
    }
    
    public void setAuthors(List<AuthorImportDecisionDTO> authors) {
        this.authors = authors;
    }
}







