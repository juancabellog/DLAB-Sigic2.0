package com.sisgic.dto;

import java.time.LocalDate;

public class EndLaboratoryMembershipRequest {
    private LocalDate endDate;

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
}
