package com.sisgic.dto;

public class AssignLabDirectorRequest {
    private Long personId;
    private String email;
    private String orcid;
    private String mobilePhone;

    public Long getPersonId() { return personId; }
    public void setPersonId(Long personId) { this.personId = personId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getOrcid() { return orcid; }
    public void setOrcid(String orcid) { this.orcid = orcid; }

    public String getMobilePhone() { return mobilePhone; }
    public void setMobilePhone(String mobilePhone) { this.mobilePhone = mobilePhone; }
}
