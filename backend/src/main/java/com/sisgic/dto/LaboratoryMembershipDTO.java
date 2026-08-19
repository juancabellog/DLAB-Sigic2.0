package com.sisgic.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class LaboratoryMembershipDTO {

    private String id;
    private Long laboratoryId;
    private Long personId;
    private String personName;
    private String personEmail;
    private String orcid;
    private String email;
    private String mobilePhone;
    private String personIniciales;
    private String profileImageUrl;
    private String membershipType;
    private String resourceType;
    private String resourceTypeLabel;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Long getLaboratoryId() { return laboratoryId; }
    public void setLaboratoryId(Long laboratoryId) { this.laboratoryId = laboratoryId; }

    public Long getPersonId() { return personId; }
    public void setPersonId(Long personId) { this.personId = personId; }

    public String getPersonName() { return personName; }
    public void setPersonName(String personName) { this.personName = personName; }

    public String getPersonEmail() { return personEmail; }
    public void setPersonEmail(String personEmail) { this.personEmail = personEmail; }

    public String getOrcid() { return orcid; }
    public void setOrcid(String orcid) { this.orcid = orcid; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMobilePhone() { return mobilePhone; }
    public void setMobilePhone(String mobilePhone) { this.mobilePhone = mobilePhone; }

    public String getPersonIniciales() { return personIniciales; }
    public void setPersonIniciales(String personIniciales) { this.personIniciales = personIniciales; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public String getMembershipType() { return membershipType; }
    public void setMembershipType(String membershipType) { this.membershipType = membershipType; }

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }

    public String getResourceTypeLabel() { return resourceTypeLabel; }
    public void setResourceTypeLabel(String resourceTypeLabel) { this.resourceTypeLabel = resourceTypeLabel; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
