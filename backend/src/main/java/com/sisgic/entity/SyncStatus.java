package com.sisgic.entity;

import jakarta.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "sync_status")
public class SyncStatus {
    
    @Id
    @Column(name = "job_key", length = 100, nullable = false)
    private String jobKey;
    
    @Column(name = "last_success_at")
    private Timestamp lastSuccessAt;
    
    @Column(name = "last_run_at")
    private Timestamp lastRunAt;
    
    @Column(name = "status")
    private Integer status;
    
    // Constructors
    public SyncStatus() {}
    
    public SyncStatus(String jobKey, Timestamp lastSuccessAt, Timestamp lastRunAt, Integer status) {
        this.jobKey = jobKey;
        this.lastSuccessAt = lastSuccessAt;
        this.lastRunAt = lastRunAt;
        this.status = status;
    }
    
    // Getters and Setters
    public String getJobKey() {
        return jobKey;
    }
    
    public void setJobKey(String jobKey) {
        this.jobKey = jobKey;
    }
    
    public Timestamp getLastSuccessAt() {
        return lastSuccessAt;
    }
    
    public void setLastSuccessAt(Timestamp lastSuccessAt) {
        this.lastSuccessAt = lastSuccessAt;
    }
    
    public Timestamp getLastRunAt() {
        return lastRunAt;
    }
    
    public void setLastRunAt(Timestamp lastRunAt) {
        this.lastRunAt = lastRunAt;
    }
    
    public Integer getStatus() {
        return status;
    }
    
    public void setStatus(Integer status) {
        this.status = status;
    }
}
