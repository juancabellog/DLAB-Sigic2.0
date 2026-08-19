package com.sisgic.dto;

import java.util.List;

/**
 * DTO for scientific product Awards (producto + award). idTipoProducto = 21.
 */
public class AwardProductDTO {

    private Long id;
    private String descripcion;
    private String comentario;
    private String fechaInicio;
    private String fechaTermino;
    private TipoProductoDTO tipoProducto;
    private String progressReport;
    private String codigoANID;
    private String basal;
    private String cluster;
    private String participantesNombres;
    private String createdAt;
    private String updatedAt;
    private List<ParticipanteDTO> participantes;

    private Integer year;
    private Long idInstitucion;
    private InstitucionDTO institucion;
    private String institutionLabel;
    private String codigoPais;
    private String countryLabel;

    public AwardProductDTO() {}

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
    public String getProgressReport() { return progressReport; }
    public void setProgressReport(String progressReport) { this.progressReport = progressReport; }
    public String getCodigoANID() { return codigoANID; }
    public void setCodigoANID(String codigoANID) { this.codigoANID = codigoANID; }
    public String getBasal() { return basal; }
    public void setBasal(String basal) { this.basal = basal; }
    public String getCluster() { return cluster; }
    public void setCluster(String cluster) { this.cluster = cluster; }
    public String getParticipantesNombres() { return participantesNombres; }
    public void setParticipantesNombres(String participantesNombres) { this.participantesNombres = participantesNombres; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    public List<ParticipanteDTO> getParticipantes() { return participantes; }
    public void setParticipantes(List<ParticipanteDTO> participantes) { this.participantes = participantes; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public Long getIdInstitucion() { return idInstitucion; }
    public void setIdInstitucion(Long idInstitucion) { this.idInstitucion = idInstitucion; }
    public InstitucionDTO getInstitucion() { return institucion; }
    public void setInstitucion(InstitucionDTO institucion) { this.institucion = institucion; }
    public String getInstitutionLabel() { return institutionLabel; }
    public void setInstitutionLabel(String institutionLabel) { this.institutionLabel = institutionLabel; }
    public String getCodigoPais() { return codigoPais; }
    public void setCodigoPais(String codigoPais) { this.codigoPais = codigoPais; }
    public String getCountryLabel() { return countryLabel; }
    public void setCountryLabel(String countryLabel) { this.countryLabel = countryLabel; }
}
