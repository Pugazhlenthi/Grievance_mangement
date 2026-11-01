package com.governance.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.LocalDateTime;

@Entity
public class Complaint {

    @Id
    private String complaintId;
    private String district;
    private String constituency; // தொகுதி (எ.கா: Anthiyur)
    private String village;
    private String category;
    private String location;
    private String description;
    private String photoPath;
    private int severityScore;
    private int reportCount;
    private LocalDateTime slaDeadline;
    private Double latitude;
    private Double longitude;
    private String status; // REGISTERED, IN_PROGRESS, RESOLVED, ESCALATED
    private LocalDateTime createdAt;

    public Complaint() {}

    public Complaint(String complaintId, String district, String constituency, String village, String category, 
                     String location, String description, String photoPath, int severityScore, 
                     LocalDateTime slaDeadline, Double latitude, Double longitude) {
        this.complaintId = complaintId;
        this.district = district;
        this.constituency = constituency;
        this.village = village;
        this.category = category;
        this.location = location;
        this.description = description;
        this.photoPath = photoPath;
        this.severityScore = severityScore;
        this.reportCount = 1;
        this.slaDeadline = slaDeadline;
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = "REGISTERED";
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getComplaintId() { return complaintId; }
    public void setComplaintId(String complaintId) { this.complaintId = complaintId; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getConstituency() { return constituency; }
    public void setConstituency(String constituency) { this.constituency = constituency; }

    public String getVillage() { return village; }
    public void setVillage(String village) { this.village = village; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }

    public int getSeverityScore() { return severityScore; }
    public void setSeverityScore(int severityScore) { this.severityScore = severityScore; }

    public int getReportCount() { return reportCount; }
    public void setReportCount(int reportCount) { this.reportCount = reportCount; }

    public LocalDateTime getSlaDeadline() { return slaDeadline; }
    public void setSlaDeadline(LocalDateTime slaDeadline) { this.slaDeadline = slaDeadline; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}