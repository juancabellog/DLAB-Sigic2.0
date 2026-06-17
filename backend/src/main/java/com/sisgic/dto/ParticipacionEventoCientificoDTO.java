package com.sisgic.dto;

import java.util.List;

public class ParticipacionEventoCientificoDTO {
    private Long id;
    private String descripcion;
    private String comentario;
    private String fechaInicio;
    private String fechaTermino;
    private TipoProductoDTO tipoProducto;
    private String linkPDF;
    private String progressReport;
    private EstadoProductoDTO estadoProducto;
    private String codigoANID;
    private String basal;
    private String cluster;
    private String createdAt;
    private String updatedAt;

    private Long idModalidadPresentacion;
    private TipoParticipacionEventoDTO tipoParticipacionEvento;
    private PaisDTO pais;
    private String ciudad;
    private String nameResearchLine;
    private String eventName;

    private List<ParticipanteDTO> participantes;

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
    public String getCluster() { return cluster; }
    public void setCluster(String cluster) { this.cluster = cluster; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    public Long getIdModalidadPresentacion() { return idModalidadPresentacion; }
    public void setIdModalidadPresentacion(Long idModalidadPresentacion) { this.idModalidadPresentacion = idModalidadPresentacion; }
    public TipoParticipacionEventoDTO getTipoParticipacionEvento() { return tipoParticipacionEvento; }
    public void setTipoParticipacionEvento(TipoParticipacionEventoDTO tipoParticipacionEvento) { this.tipoParticipacionEvento = tipoParticipacionEvento; }
    public PaisDTO getPais() { return pais; }
    public void setPais(PaisDTO pais) { this.pais = pais; }
    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }
    public String getNameResearchLine() { return nameResearchLine; }
    public void setNameResearchLine(String nameResearchLine) { this.nameResearchLine = nameResearchLine; }
    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }
    public List<ParticipanteDTO> getParticipantes() { return participantes; }
    public void setParticipantes(List<ParticipanteDTO> participantes) { this.participantes = participantes; }
}
