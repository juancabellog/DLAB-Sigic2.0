package com.sisgic.dto;

import java.util.List;

/**
 * DTO for scientific product Projects (producto + proyecto).
 */
public class ProjectProductDTO {

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
    private String mainResponsible;
    private String createdAt;
    private String updatedAt;
    private List<ParticipanteDTO> participantes;

    private String projectCode;
    private String awardDate;
    private Integer duration;
    private Integer totalAmount;
    private Integer totalAmountCenter;
    private FundingTypeDTO fundingType;
    private Long idFundingtype;
    private String otherFundingType;
    private String projectTypes;
    private String otherProjectType;
    private String nameSocialOrganizations;
    private String namePublicSectorEntities;
    private String namePrivateSectorEntities;
    private String nameTradeRegionalAssociations;
    private String nameSTEntities;

    /** Resolved labels for list/detail (comma-separated). */
    private String projectTypesLabels;
    private String fundingTypeLabel;

    public ProjectProductDTO() {}

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
    public String getMainResponsible() { return mainResponsible; }
    public void setMainResponsible(String mainResponsible) { this.mainResponsible = mainResponsible; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    public List<ParticipanteDTO> getParticipantes() { return participantes; }
    public void setParticipantes(List<ParticipanteDTO> participantes) { this.participantes = participantes; }

    public String getProjectCode() { return projectCode; }
    public void setProjectCode(String projectCode) { this.projectCode = projectCode; }
    public String getAwardDate() { return awardDate; }
    public void setAwardDate(String awardDate) { this.awardDate = awardDate; }
    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
    public Integer getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Integer totalAmount) { this.totalAmount = totalAmount; }
    public Integer getTotalAmountCenter() { return totalAmountCenter; }
    public void setTotalAmountCenter(Integer totalAmountCenter) { this.totalAmountCenter = totalAmountCenter; }
    public FundingTypeDTO getFundingType() { return fundingType; }
    public void setFundingType(FundingTypeDTO fundingType) { this.fundingType = fundingType; }
    public Long getIdFundingtype() { return idFundingtype; }
    public void setIdFundingtype(Long idFundingtype) { this.idFundingtype = idFundingtype; }
    public String getOtherFundingType() { return otherFundingType; }
    public void setOtherFundingType(String otherFundingType) { this.otherFundingType = otherFundingType; }
    public String getProjectTypes() { return projectTypes; }
    public void setProjectTypes(String projectTypes) { this.projectTypes = projectTypes; }
    public String getOtherProjectType() { return otherProjectType; }
    public void setOtherProjectType(String otherProjectType) { this.otherProjectType = otherProjectType; }
    public String getNameSocialOrganizations() { return nameSocialOrganizations; }
    public void setNameSocialOrganizations(String nameSocialOrganizations) { this.nameSocialOrganizations = nameSocialOrganizations; }
    public String getNamePublicSectorEntities() { return namePublicSectorEntities; }
    public void setNamePublicSectorEntities(String namePublicSectorEntities) { this.namePublicSectorEntities = namePublicSectorEntities; }
    public String getNamePrivateSectorEntities() { return namePrivateSectorEntities; }
    public void setNamePrivateSectorEntities(String namePrivateSectorEntities) { this.namePrivateSectorEntities = namePrivateSectorEntities; }
    public String getNameTradeRegionalAssociations() { return nameTradeRegionalAssociations; }
    public void setNameTradeRegionalAssociations(String nameTradeRegionalAssociations) { this.nameTradeRegionalAssociations = nameTradeRegionalAssociations; }
    public String getNameSTEntities() { return nameSTEntities; }
    public void setNameSTEntities(String nameSTEntities) { this.nameSTEntities = nameSTEntities; }
    public String getProjectTypesLabels() { return projectTypesLabels; }
    public void setProjectTypesLabels(String projectTypesLabels) { this.projectTypesLabels = projectTypesLabels; }
    public String getFundingTypeLabel() { return fundingTypeLabel; }
    public void setFundingTypeLabel(String fundingTypeLabel) { this.fundingTypeLabel = fundingTypeLabel; }
}
