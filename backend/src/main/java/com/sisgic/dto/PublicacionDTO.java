package com.sisgic.dto;

import java.util.List;

/**
 * DTO para Publicación
 * Extiende ProductoCientificoDTO y agrega campos específicos de publicación
 */
public class PublicacionDTO {
    
    // Campos de ProductoCientifico
    private Long id;
    private String descripcion;
    private String comentario;
    private String fechaInicio; // LocalDate como string (formato ISO: YYYY-MM-DD)
    private String fechaTermino; // LocalDate como string (formato ISO: YYYY-MM-DD)
    private TipoProductoDTO tipoProducto;
    private String urlDocumento;
    private String linkVisualizacion;
    private String linkPDF;
    private String progressReport;
    private EstadoProductoDTO estadoProducto;
    private String codigoANID;
    private String basal; // Character como string
    private String lineasInvestigacion; // JSON string
    private String participantesNombres; // Nombres de participantes concatenados por coma (campo calculado)
    private String createdAt;
    private String updatedAt;
    private String cluster; // Lista de IDs de cluster separados por comas
    
    // Campos específicos de Publicacion
    private JournalDTO journal;
    private String volume;
    private Integer yearPublished;
    private String firstpage;
    private String lastpage;
    private String indexs; // JSON string
    private String funding; // JSON string
    private String doi;
    private Integer numCitas;
    private Double factorImpacto; // Factor de Impacto del Journal
    private Double factorImpactoPromedio; // Factor de Impacto Promedio del Journal
    
    // Participantes
    private List<ParticipanteDTO> participantes;
    
    // Constructors
    public PublicacionDTO() {}
    
    // Getters and Setters - ProductoCientifico
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
    
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
    
    public String getComentario() {
        return comentario;
    }
    
    public void setComentario(String comentario) {
        this.comentario = comentario;
    }
    
    public String getFechaInicio() {
        return fechaInicio;
    }
    
    public void setFechaInicio(String fechaInicio) {
        this.fechaInicio = fechaInicio;
    }
    
    public String getFechaTermino() {
        return fechaTermino;
    }
    
    public void setFechaTermino(String fechaTermino) {
        this.fechaTermino = fechaTermino;
    }
    
    public TipoProductoDTO getTipoProducto() {
        return tipoProducto;
    }
    
    public void setTipoProducto(TipoProductoDTO tipoProducto) {
        this.tipoProducto = tipoProducto;
    }
    
    public String getUrlDocumento() {
        return urlDocumento;
    }
    
    public void setUrlDocumento(String urlDocumento) {
        this.urlDocumento = urlDocumento;
    }
    
    public String getLinkVisualizacion() {
        return linkVisualizacion;
    }
    
    public void setLinkVisualizacion(String linkVisualizacion) {
        this.linkVisualizacion = linkVisualizacion;
    }
    
    public String getLinkPDF() {
        return linkPDF;
    }
    
    public void setLinkPDF(String linkPDF) {
        this.linkPDF = linkPDF;
    }
    
    public String getProgressReport() {
        return progressReport;
    }
    
    public void setProgressReport(String progressReport) {
        this.progressReport = progressReport;
    }
    
    public EstadoProductoDTO getEstadoProducto() {
        return estadoProducto;
    }
    
    public void setEstadoProducto(EstadoProductoDTO estadoProducto) {
        this.estadoProducto = estadoProducto;
    }
    
    public String getCodigoANID() {
        return codigoANID;
    }
    
    public void setCodigoANID(String codigoANID) {
        this.codigoANID = codigoANID;
    }
    
    public String getBasal() {
        return basal;
    }
    
    public void setBasal(String basal) {
        this.basal = basal;
    }
    
    public String getLineasInvestigacion() {
        return lineasInvestigacion;
    }
    
    public void setLineasInvestigacion(String lineasInvestigacion) {
        this.lineasInvestigacion = lineasInvestigacion;
    }
    
    public String getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public String getCluster() {
        return cluster;
    }
    
    public void setCluster(String cluster) {
        this.cluster = cluster;
    }
    
    // Getters and Setters - Publicacion específicos
    public JournalDTO getJournal() {
        return journal;
    }
    
    public void setJournal(JournalDTO journal) {
        this.journal = journal;
    }
    
    public String getVolume() {
        return volume;
    }
    
    public void setVolume(String volume) {
        this.volume = volume;
    }
    
    public Integer getYearPublished() {
        return yearPublished;
    }
    
    public void setYearPublished(Integer yearPublished) {
        this.yearPublished = yearPublished;
    }
    
    public String getFirstpage() {
        return firstpage;
    }
    
    public void setFirstpage(String firstpage) {
        this.firstpage = firstpage;
    }
    
    public String getLastpage() {
        return lastpage;
    }
    
    public void setLastpage(String lastpage) {
        this.lastpage = lastpage;
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
    
    public String getDoi() {
        return doi;
    }
    
    public void setDoi(String doi) {
        this.doi = doi;
    }
    
    public Integer getNumCitas() {
        return numCitas;
    }
    
    public void setNumCitas(Integer numCitas) {
        this.numCitas = numCitas;
    }
    
    public Double getFactorImpacto() {
        return factorImpacto;
    }
    
    public void setFactorImpacto(Double factorImpacto) {
        this.factorImpacto = factorImpacto;
    }
    
    public Double getFactorImpactoPromedio() {
        return factorImpactoPromedio;
    }
    
    public void setFactorImpactoPromedio(Double factorImpactoPromedio) {
        this.factorImpactoPromedio = factorImpactoPromedio;
    }
    
    public List<ParticipanteDTO> getParticipantes() {
        return participantes;
    }
    
    public void setParticipantes(List<ParticipanteDTO> participantes) {
        this.participantes = participantes;
    }
    
    public String getParticipantesNombres() {
        return participantesNombres;
    }
    
    public void setParticipantesNombres(String participantesNombres) {
        this.participantesNombres = participantesNombres;
    }
}

