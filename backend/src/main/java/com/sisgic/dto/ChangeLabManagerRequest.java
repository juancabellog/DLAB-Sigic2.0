package com.sisgic.dto;

import java.time.LocalDate;

public class ChangeLabManagerRequest {
    private Long personId;
    private String resourceType;
    private LocalDate startDate;
    private LocalDate endDateForCurrent;
    private String email;
    private String orcid;
    private String mobilePhone;

    public Long getPersonId() { return personId; }
    public void setPersonId(Long personId) { this.personId = personId; }

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDateForCurrent() { return endDateForCurrent; }
    public void setEndDateForCurrent(LocalDate endDateForCurrent) { this.endDateForCurrent = endDateForCurrent; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getOrcid() { return orcid; }
    public void setOrcid(String orcid) { this.orcid = orcid; }

    public String getMobilePhone() { return mobilePhone; }
    public void setMobilePhone(String mobilePhone) { this.mobilePhone = mobilePhone; }
}
