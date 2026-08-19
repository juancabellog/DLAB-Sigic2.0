package com.sisgic.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Formula;

import java.time.LocalDate;

/**
 * Scientific product subtype mapped to table {@code proyecto}.
 * Shares primary key with {@code producto}.
 */
@Entity
@Table(name = "proyecto")
@PrimaryKeyJoinColumn(name = "id")
public class ProyectoCientifico extends ProductoCientifico {

    @Column(name = "projectCode", nullable = false, length = 100)
    private String projectCode;

    @Column(name = "awardDate", nullable = false)
    private LocalDate awardDate;

    @Column(name = "duration", nullable = false)
    private Integer duration;

    @Column(name = "totalAmount", nullable = false)
    private Integer totalAmount;

    @Column(name = "totalAmountCenter", nullable = false)
    private Integer totalAmountCenter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idFundingtype", nullable = false)
    private FundingType fundingType;

    @Column(name = "otherFundingType", columnDefinition = "TINYTEXT")
    private String otherFundingType;

    /** Comma-separated tipoproyecto IDs (e.g. "1,4"). Id 4 = Other. */
    @Column(name = "projectTypes", nullable = false, columnDefinition = "TINYTEXT")
    private String projectTypes;

    @Column(name = "otherProjectType", columnDefinition = "TINYTEXT")
    private String otherProjectType;

    @Column(name = "nameSocialOrganizations", columnDefinition = "TINYTEXT")
    private String nameSocialOrganizations;

    @Column(name = "namePublicSectorEntities", columnDefinition = "TINYTEXT")
    private String namePublicSectorEntities;

    @Column(name = "namePrivateSectorEntities", columnDefinition = "TINYTEXT")
    private String namePrivateSectorEntities;

    @Column(name = "nameTradeRegionalAssociations", columnDefinition = "TINYTEXT")
    private String nameTradeRegionalAssociations;

    @Column(name = "nameSTEntities", columnDefinition = "TINYTEXT")
    private String nameSTEntities;

    @Formula("(SELECT f_getParticipantByRol(id, 20))")
    private String mainResponsible;

    public ProyectoCientifico() {}

    public String getProjectCode() {
        return projectCode;
    }

    public void setProjectCode(String projectCode) {
        this.projectCode = projectCode;
    }

    public LocalDate getAwardDate() {
        return awardDate;
    }

    public void setAwardDate(LocalDate awardDate) {
        this.awardDate = awardDate;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public Integer getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Integer totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Integer getTotalAmountCenter() {
        return totalAmountCenter;
    }

    public void setTotalAmountCenter(Integer totalAmountCenter) {
        this.totalAmountCenter = totalAmountCenter;
    }

    public FundingType getFundingType() {
        return fundingType;
    }

    public void setFundingType(FundingType fundingType) {
        this.fundingType = fundingType;
    }

    public String getOtherFundingType() {
        return otherFundingType;
    }

    public void setOtherFundingType(String otherFundingType) {
        this.otherFundingType = otherFundingType;
    }

    public String getProjectTypes() {
        return projectTypes;
    }

    public void setProjectTypes(String projectTypes) {
        this.projectTypes = projectTypes;
    }

    public String getOtherProjectType() {
        return otherProjectType;
    }

    public void setOtherProjectType(String otherProjectType) {
        this.otherProjectType = otherProjectType;
    }

    public String getNameSocialOrganizations() {
        return nameSocialOrganizations;
    }

    public void setNameSocialOrganizations(String nameSocialOrganizations) {
        this.nameSocialOrganizations = nameSocialOrganizations;
    }

    public String getNamePublicSectorEntities() {
        return namePublicSectorEntities;
    }

    public void setNamePublicSectorEntities(String namePublicSectorEntities) {
        this.namePublicSectorEntities = namePublicSectorEntities;
    }

    public String getNamePrivateSectorEntities() {
        return namePrivateSectorEntities;
    }

    public void setNamePrivateSectorEntities(String namePrivateSectorEntities) {
        this.namePrivateSectorEntities = namePrivateSectorEntities;
    }

    public String getNameTradeRegionalAssociations() {
        return nameTradeRegionalAssociations;
    }

    public void setNameTradeRegionalAssociations(String nameTradeRegionalAssociations) {
        this.nameTradeRegionalAssociations = nameTradeRegionalAssociations;
    }

    public String getNameSTEntities() {
        return nameSTEntities;
    }

    public void setNameSTEntities(String nameSTEntities) {
        this.nameSTEntities = nameSTEntities;
    }

    public String getMainResponsible() {
        return mainResponsible;
    }

    public void setMainResponsible(String mainResponsible) {
        this.mainResponsible = mainResponsible;
    }
}
