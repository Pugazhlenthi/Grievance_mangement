package com.governance.controller;

import com.governance.model.Complaint;
import com.governance.model.ComplaintHistory;
import com.governance.repository.ComplaintHistoryRepository;
import com.governance.repository.ComplaintRepository;
import com.governance.service.DataCleanupScheduler;
import com.governance.service.SeverityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class GrievanceController {

    @Autowired
    private ComplaintRepository repository;

    @Autowired
    private ComplaintHistoryRepository historyRepository;

    @Autowired
    private SeverityService severityService;

    @Autowired
    private DataCleanupScheduler cleanupScheduler;

    @PostMapping(value = "/complaints", consumes = {"multipart/form-data"})
    public Complaint registerComplaint(
            @RequestParam("district") String district,
            @RequestParam("constituency") String constituency,
            @RequestParam("village") String village,
            @RequestParam("category") String category,
            @RequestParam("location") String location,
            @RequestParam("description") String description,
            @RequestParam(value = "latitude", required = false) Double latitude,
            @RequestParam(value = "longitude", required = false) Double longitude,
            @RequestParam("image") MultipartFile image) throws IOException {

        // Evidence photo is mandatory validation
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Evidence Photo is mandatory to register a complaint.");
        }

        String uploadDir = System.getProperty("user.dir") + "/uploads/";
        File dir = new File(uploadDir);
        if (!dir.exists()) dir.mkdirs();
        
        String imageFileName = System.currentTimeMillis() + "_" + image.getOriginalFilename();
        image.transferTo(new File(uploadDir + imageFileName));

        String formattedLocation = location.trim().replaceAll("\\s+", " ");
        String formattedVillage = village.trim().replaceAll("\\s+", " ");
        String formattedConstituency = constituency.trim().replaceAll("\\s+", " ");

        Optional<Complaint> existing = repository.findByConstituencyAndVillageAndCategoryAndLocationAndStatusNot(
                formattedConstituency, formattedVillage, category, formattedLocation, "RESOLVED");

        if (existing.isPresent()) {
            Complaint complaint = existing.get();
            complaint.setReportCount(complaint.getReportCount() + 1);
            complaint.setSeverityScore(Math.min(complaint.getSeverityScore() + 1, 5));
            return repository.save(complaint);
        } else {
            String id = "G" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
            int severity = severityService.calculateSeverity(description, category);
            
            LocalDateTime slaDeadline = LocalDateTime.now();
            if ("WATER_SUPPLY".equalsIgnoreCase(category)) {
                slaDeadline = slaDeadline.plusHours(24);
            } else if ("ELECTRICITY".equalsIgnoreCase(category)) {
                slaDeadline = slaDeadline.plusHours(48);
            } else if ("ROADS".equalsIgnoreCase(category)) {
                slaDeadline = slaDeadline.plusHours(72);
            } else {
                slaDeadline = slaDeadline.plusHours(24);
            }

            Complaint newComplaint = new Complaint(id, district, formattedConstituency, formattedVillage, category, formattedLocation, description, imageFileName, severity, slaDeadline, latitude, longitude);
            Complaint saved = repository.save(newComplaint);
            historyRepository.save(new ComplaintHistory(id, "NONE", "REGISTERED"));
            return saved;
        }
    }

    @GetMapping("/complaints/track/{id}")
    public Complaint trackComplaint(@PathVariable String id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Complaint not found"));
    }

    @GetMapping("/department/{constituency}/{category}")
    public List<Complaint> getConstituencyDepartmentQueue(
            @PathVariable String constituency, 
            @PathVariable String category) {
        if ("ALL".equalsIgnoreCase(category)) {
            return repository.findByConstituencyOrderBySeverityScoreDesc(constituency);
        }
        return repository.findByConstituencyAndCategoryOrderBySeverityScoreDesc(constituency, category);
    }

    @PutMapping("/complaints/{id}/status")
    public Complaint updateStatus(@PathVariable String id, @RequestBody Map<String, String> payload) {
        Complaint complaint = repository.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
        String oldStatus = complaint.getStatus();
        String newStatus = payload.get("status");

        complaint.setStatus(newStatus);
        Complaint updated = repository.save(complaint);
        historyRepository.save(new ComplaintHistory(id, oldStatus, newStatus));
        return updated;
    }

    @GetMapping("/admin/{district}/escalations")
    public List<Complaint> getDistrictEscalatedComplaints(@PathVariable String district) {
        return repository.findByDistrictAndStatus(district, "ESCALATED");
    }

    @DeleteMapping("/complaints/{id}")
    public String deleteComplaint(@PathVariable String id) {
        Optional<Complaint> optionalComplaint = repository.findById(id);
        if (optionalComplaint.isPresent()) {
            Complaint complaint = optionalComplaint.get();
            cleanupScheduler.deletePhotoFile(complaint.getPhotoPath());
            repository.deleteById(id);
            return "Complaint deleted successfully";
        } else {
            throw new RuntimeException("Complaint not found with ID: " + id);
        }
    }
}