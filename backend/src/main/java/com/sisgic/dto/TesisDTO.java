package com.sisgic.dto;

import java.util.List;

/**
 * DTO para Tesis
 * Extiende ProductoCientificoDTO y agrega campos específicos
 */
public class TesisDTO {
    
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
    private Integer progressReport;
    private EstadoProductoDTO estadoProducto;
    private String codigoANID;
    private String basal; // Character como string
    private String lineasInvestigacion; // JSON string
    private String participantesNombres; // Nombres de participantes concatenados por coma (campo calculado)
    private String createdAt;
    private String updatedAt;
    
    // Campos específicos de Tesis
    private InstitucionDTO institucionOG; // Institución que otorga el título
    private GradoAcademicoDTO gradoAcademico;
    private InstitucionDTO institucion; // Institución donde se insertó el estudiante
    private EstadoTesisDTO estadoTesis;
    private String fechaInicioPrograma; // LocalDate como string
    private String nombreCompletoTitulo;
    private String tipoSector; // JSON string o lista separada por comas
    private String estudiante; // Campo calculado: nombre del estudiante (rol 7)
    
    // Participantes
    private List<ParticipanteDTO> participantes;
    
    // Constructors
    public TesisDTO() {}
    
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
    public Integer getProgressReport() { return progressReport; }
    public void setProgressReport(Integer progressReport) { this.progressReport = progressReport; }
    public EstadoProductoDTO getEstadoProducto() { return estadoProducto; }
    public void setEstadoProducto(EstadoProductoDTO estadoProducto) { this.estadoProducto = estadoProducto; }
    public String getCodigoANID() { return codigoANID; }
    public void setCodigoANID(String codigoANID) { this.codigoANID = codigoANID; }
    public String getBasal() { return basal; }
    public void setBasal(String basal) { this.basal = basal; }
    public String getLineasInvestigacion() { return lineasInvestigacion; }
    public void setLineasInvestigacion(String lineasInvestigacion) { this.lineasInvestigacion = lineasInvestigacion; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    
    // Getters and Setters - Tesis específicos
    public InstitucionDTO getInstitucionOG() { return institucionOG; }
    public void setInstitucionOG(InstitucionDTO institucionOG) { this.institucionOG = institucionOG; }
    public GradoAcademicoDTO getGradoAcademico() { return gradoAcademico; }
    public void setGradoAcademico(GradoAcademicoDTO gradoAcademico) { this.gradoAcademico = gradoAcademico; }
    public InstitucionDTO getInstitucion() { return institucion; }
    public void setInstitucion(InstitucionDTO institucion) { this.institucion = institucion; }
    public EstadoTesisDTO getEstadoTesis() { return estadoTesis; }
    public void setEstadoTesis(EstadoTesisDTO estadoTesis) { this.estadoTesis = estadoTesis; }
    public String getFechaInicioPrograma() { return fechaInicioPrograma; }
    public void setFechaInicioPrograma(String fechaInicioPrograma) { this.fechaInicioPrograma = fechaInicioPrograma; }
    public String getNombreCompletoTitulo() { return nombreCompletoTitulo; }
    public void setNombreCompletoTitulo(String nombreCompletoTitulo) { this.nombreCompletoTitulo = nombreCompletoTitulo; }
    public String getTipoSector() { return tipoSector; }
    public void setTipoSector(String tipoSector) { this.tipoSector = tipoSector; }
    public String getEstudiante() { return estudiante; }
    public void setEstudiante(String estudiante) { this.estudiante = estudiante; }
    
    // Getters and Setters - Participantes
    public List<ParticipanteDTO> getParticipantes() { return participantes; }
    public void setParticipantes(List<ParticipanteDTO> participantes) { this.participantes = participantes; }
    
    public String getParticipantesNombres() { return participantesNombres; }
    public void setParticipantesNombres(String participantesNombres) { this.participantesNombres = participantesNombres; }
}







