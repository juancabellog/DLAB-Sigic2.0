package com.sisgic.dto;

import java.util.List;

/**
 * DTO para BecariosPostdoctorales
 * Extiende ProductoCientificoDTO y agrega campos específicos
 */
public class BecariosPostdoctoralesDTO {
    
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
    private String cluster; // Lista de IDs de cluster separados por comas
    private String participantesNombres; // Nombres de participantes concatenados por coma (campo calculado)
    private String createdAt;
    private String updatedAt;
    
    // Campos específicos de BecariosPostdoctorales
    private InstitucionDTO institucion;
    private String fundingSource; // JSON string o lista separada por comas
    private TipoSectorDTO tipoSector;
    private String resources; // JSON string o lista separada por comas
    private String postdoctoralFellowName; // Campo calculado: nombre del becario postdoctoral (rol 19)
    
    // Participantes
    private List<ParticipanteDTO> participantes;
    
    // Constructors
    public BecariosPostdoctoralesDTO() {}
    
    // Getters and Setters - ProductoCientifico
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }
    public String getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(String fechaInicio) { this.fechaInicio = fechaInicio; }
    public String getFechaTermino() { return fechaTermino; }
    public void setFechaTermino(String fechaTermino) { this.fechaTermino = fechaTermino; }
    public TipoProductoDTO getTipoProducto() { return tipoProducto; }
    public void setTipoProducto(TipoProductoDTO tipoProducto) { this.tipoProducto = tipoProducto; }
    public String getUrlDocumento() { return urlDocumento; }
    public void setUrlDocumento(String urlDocumento) { this.urlDocumento = urlDocumento; }
    public String getLinkVisualizacion() { return linkVisualizacion; }
    public void setLinkVisualizacion(String linkVisualizacion) { this.linkVisualizacion = linkVisualizacion; }
    public String getLinkPDF() { return linkPDF; }
    public void setLinkPDF(String linkPDF) { this.linkPDF = linkPDF; }
    public String getProgressReport() { return progressReport; }
    public void setProgressReport(String progressReport) { this.progressReport = progressReport; }
    public EstadoProductoDTO getEstadoProducto() { return estadoProducto; }
    public void setEstadoProducto(EstadoProductoDTO estadoProducto) { this.estadoProducto = estadoProducto; }
    public String getCodigoANID() { return codigoANID; }
    public void setCodigoANID(String codigoANID) { this.codigoANID = codigoANID; }
    public String getBasal() { return basal; }
    public void setBasal(String basal) { this.basal = basal; }
    public String getLineasInvestigacion() { return lineasInvestigacion; }
    public void setLineasInvestigacion(String lineasInvestigacion) { this.lineasInvestigacion = lineasInvestigacion; }
    public String getCluster() { return cluster; }
    public void setCluster(String cluster) { this.cluster = cluster; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    
    // Getters and Setters - BecariosPostdoctorales específicos
    public InstitucionDTO getInstitucion() { return institucion; }
    public void setInstitucion(InstitucionDTO institucion) { this.institucion = institucion; }
    public String getFundingSource() { return fundingSource; }
    public void setFundingSource(String fundingSource) { this.fundingSource = fundingSource; }
    public TipoSectorDTO getTipoSector() { return tipoSector; }
    public void setTipoSector(TipoSectorDTO tipoSector) { this.tipoSector = tipoSector; }
    public String getResources() { return resources; }
    public void setResources(String resources) { this.resources = resources; }
    public String getPostdoctoralFellowName() { return postdoctoralFellowName; }
    public void setPostdoctoralFellowName(String postdoctoralFellowName) { this.postdoctoralFellowName = postdoctoralFellowName; }
    
    // Getters and Setters - Participantes
    public List<ParticipanteDTO> getParticipantes() { return participantes; }
    public void setParticipantes(List<ParticipanteDTO> participantes) { this.participantes = participantes; }
    
    public String getParticipantesNombres() { return participantesNombres; }
    public void setParticipantesNombres(String participantesNombres) { this.participantesNombres = participantesNombres; }
}







