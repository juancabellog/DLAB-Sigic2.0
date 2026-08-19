package com.sisgic.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class LaboratoryDTO {

    private Long id;
    private String nameEs;
    private String nameEn;
    private String descriptionEs;
    private String descriptionEn;
    private String imageUrl;
    private String imageAltEs;
    private String imageAltEn;
    private Integer clusterId;
    private String clusterLabel;
    private Long directorId;
    private String directorName;
    private String directorEmail;
    private String directorOrcid;
    private String directorMobilePhone;
    private String directorIniciales;
    private String directorProfileImageUrl;
    private String directorResourceType;
    private String directorResourceTypeLabel;
    private String status;
    private String translationStatus;
    private LocalDateTime translationValidatedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String slug;
    private String metaTitle;
    private String metaDescription;
    private String ogTitle;
    private String ogDescription;
    private String ogImageUrl;
    private String publicUrl;

    private Integer activeMemberCount;
    private String labManagerName;

    private List<LaboratoryMembershipDTO> memberships = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNameEs() { return nameEs; }
    public void setNameEs(String nameEs) { this.nameEs = nameEs; }

    public String getNameEn() { return nameEn; }
    public void setNameEn(String nameEn) { this.nameEn = nameEn; }

    public String getDescriptionEs() { return descriptionEs; }
    public void setDescriptionEs(String descriptionEs) { this.descriptionEs = descriptionEs; }

    public String getDescriptionEn() { return descriptionEn; }
    public void setDescriptionEn(String descriptionEn) { this.descriptionEn = descriptionEn; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getImageAltEs() { return imageAltEs; }
    public void setImageAltEs(String imageAltEs) { this.imageAltEs = imageAltEs; }

    public String getImageAltEn() { return imageAltEn; }
    public void setImageAltEn(String imageAltEn) { this.imageAltEn = imageAltEn; }

    public Integer getClusterId() { return clusterId; }
    public void setClusterId(Integer clusterId) { this.clusterId = clusterId; }

    public String getClusterLabel() { return clusterLabel; }
    public void setClusterLabel(String clusterLabel) { this.clusterLabel = clusterLabel; }

    public Long getDirectorId() { return directorId; }
    public void setDirectorId(Long directorId) { this.directorId = directorId; }

    public String getDirectorName() { return directorName; }
    public void setDirectorName(String directorName) { this.directorName = directorName; }

    public String getDirectorEmail() { return directorEmail; }
    public void setDirectorEmail(String directorEmail) { this.directorEmail = directorEmail; }

    public String getDirectorOrcid() { return directorOrcid; }
    public void setDirectorOrcid(String directorOrcid) { this.directorOrcid = directorOrcid; }

    public String getDirectorMobilePhone() { return directorMobilePhone; }
    public void setDirectorMobilePhone(String directorMobilePhone) { this.directorMobilePhone = directorMobilePhone; }

    public String getDirectorIniciales() { return directorIniciales; }
    public void setDirectorIniciales(String directorIniciales) { this.directorIniciales = directorIniciales; }

    public String getDirectorProfileImageUrl() { return directorProfileImageUrl; }
    public void setDirectorProfileImageUrl(String directorProfileImageUrl) { this.directorProfileImageUrl = directorProfileImageUrl; }

    public String getDirectorResourceType() { return directorResourceType; }
    public void setDirectorResourceType(String directorResourceType) { this.directorResourceType = directorResourceType; }

    public String getDirectorResourceTypeLabel() { return directorResourceTypeLabel; }
    public void setDirectorResourceTypeLabel(String directorResourceTypeLabel) { this.directorResourceTypeLabel = directorResourceTypeLabel; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTranslationStatus() { return translationStatus; }
    public void setTranslationStatus(String translationStatus) { this.translationStatus = translationStatus; }

    public LocalDateTime getTranslationValidatedAt() { return translationValidatedAt; }
    public void setTranslationValidatedAt(LocalDateTime translationValidatedAt) { this.translationValidatedAt = translationValidatedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getMetaTitle() { return metaTitle; }
    public void setMetaTitle(String metaTitle) { this.metaTitle = metaTitle; }

    public String getMetaDescription() { return metaDescription; }
    public void setMetaDescription(String metaDescription) { this.metaDescription = metaDescription; }

    public String getOgTitle() { return ogTitle; }
    public void setOgTitle(String ogTitle) { this.ogTitle = ogTitle; }

    public String getOgDescription() { return ogDescription; }
    public void setOgDescription(String ogDescription) { this.ogDescription = ogDescription; }

    public String getOgImageUrl() { return ogImageUrl; }
    public void setOgImageUrl(String ogImageUrl) { this.ogImageUrl = ogImageUrl; }

    public String getPublicUrl() { return publicUrl; }
    public void setPublicUrl(String publicUrl) { this.publicUrl = publicUrl; }

    public Integer getActiveMemberCount() { return activeMemberCount; }
    public void setActiveMemberCount(Integer activeMemberCount) { this.activeMemberCount = activeMemberCount; }

    public String getLabManagerName() { return labManagerName; }
    public void setLabManagerName(String labManagerName) { this.labManagerName = labManagerName; }

    public List<LaboratoryMembershipDTO> getMemberships() { return memberships; }
    public void setMemberships(List<LaboratoryMembershipDTO> memberships) {
        this.memberships = memberships != null ? memberships : new ArrayList<>();
    }
}
