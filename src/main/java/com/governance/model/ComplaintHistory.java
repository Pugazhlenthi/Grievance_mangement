package com.governance.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class ComplaintHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String complaintId;
    private String oldStatus;
    private String newStatus;
    private LocalDateTime changedAt;

    public ComplaintHistory() {}

    public ComplaintHistory(String complaintId, String oldStatus, String newStatus) {
        this.complaintId = complaintId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.changedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getComplaintId() { return complaintId; }
    public String getOldStatus() { return oldStatus; }
    public String getNewStatus() { return newStatus; }
    public LocalDateTime getChangedAt() { return changedAt; }
}