package com.governance.controller;

import com.governance.model.Complaint;
import com.governance.model.ComplaintHistory;
import com.governance.repository.ComplaintHistoryRepository;
import com.governance.repository.ComplaintRepository;
import com.governance.service.GeoUtils;
import com.governance.service.SeverityService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

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

    // 1. Complaint Registration with 30-Meter GPS Duplicate Check
    @PostMapping("/complaints")
    public ResponseEntity<?> registerComplaint(
            @RequestParam("district") String district,
            @RequestParam("constituency") String constituency,
            @RequestParam("village") String village,
            @RequestParam("category") String category,
            @RequestParam("location") String location,
            @RequestParam("description") String description,
            @RequestParam(value = "latitude", required = false) Double latitude,
            @RequestParam(value = "longitude", required = false) Double longitude,
            @RequestParam(value = "image", required = false) MultipartFile image) {

        if (image == null || image.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Evidence Photo is mandatory to register a complaint.");
        }

        String cleanConstituency = constituency.trim();
        String cleanVillage = village.trim();
        String cleanCategory = category.trim();

        // Check for existing active complaints in the same constituency and category
        List<Complaint> existingComplaints = repository.findAll();

        for (Complaint existing : existingComplaints) {
            boolean sameConstituency = cleanConstituency.equalsIgnoreCase(existing.getConstituency());
            boolean sameCategory = cleanCategory.equalsIgnoreCase(existing.getCategory());
            boolean notResolved = !"RESOLVED".equalsIgnoreCase(existing.getStatus());

            if (sameConstituency && sameCategory && notResolved) {
                double distance = GeoUtils.calculateDistance(
                        existing.getLatitude(), existing.getLongitude(),
                        latitude, longitude
                );

                boolean nearGps = distance <= 30.0;
                boolean exactTextMatch = cleanVillage.equalsIgnoreCase(existing.getVillage()) &&
                                         location.trim().equalsIgnoreCase(existing.getLocation());

                if (nearGps || exactTextMatch) {
                    existing.setReportCount(existing.getReportCount() + 1);
                    int newSeverity = Math.min(5, existing.getSeverityScore() + 1);
                    existing.setSeverityScore(newSeverity);

                    repository.save(existing);
                    return ResponseEntity.ok(existing);
                }
            }
        }

        // Save new complaint if no match found
        String photoPath = saveUploadedFile(image);

        Complaint complaint = new Complaint();
        complaint.setComplaintId("G" + (10000 + new Random().nextInt(90000)));
        complaint.setDistrict(district.trim());
        complaint.setConstituency(cleanConstituency);
        complaint.setVillage(cleanVillage);
        complaint.setCategory(cleanCategory);
        complaint.setLocation(location.trim());
        complaint.setDescription(description.trim());
        complaint.setPhotoPath(photoPath);
        complaint.setLatitude(latitude);
        complaint.setLongitude(longitude);
        complaint.setStatus("REGISTERED");
        complaint.setReportCount(1);
        complaint.setCreatedAt(LocalDateTime.now());

        int initialSeverity = severityService.calculateSeverity(description, cleanCategory);
        complaint.setSeverityScore(initialSeverity);

        int slaHours = getSlaHoursForCategory(cleanCategory);
        complaint.setSlaDeadline(LocalDateTime.now().plusHours(slaHours));

        repository.save(complaint);
        historyRepository.save(new ComplaintHistory(complaint.getComplaintId(), "NONE", "REGISTERED"));

        return ResponseEntity.ok(complaint);
    }

    // 2. Department Queue Endpoint (Case-Insensitive Constituency & Category Filter)
    @GetMapping("/department/{constituency}/{category}")
    public ResponseEntity<List<Complaint>> getConstituencyQueue(
            @PathVariable("constituency") String constituency,
            @PathVariable("category") String category) {

        String cleanConstituency = constituency.trim();
        List<Complaint> list;

        if ("ALL".equalsIgnoreCase(category.trim())) {
            list = repository.findByConstituencyIgnoreCaseOrderBySeverityScoreDesc(cleanConstituency);
        } else {
            list = repository.findByConstituencyIgnoreCaseAndCategoryIgnoreCaseOrderBySeverityScoreDesc(cleanConstituency, category.trim());
        }

        return ResponseEntity.ok(list);
    }

    // 3. Complaint Tracking Endpoint
    @GetMapping("/complaints/track/{id}")
    public ResponseEntity<?> trackComplaint(@PathVariable("id") String id) {
        Optional<Complaint> complaint = repository.findById(id);
        if (complaint.isPresent()) {
            return ResponseEntity.ok(complaint.get());
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Complaint ID not found");
    }

    // 4. Update Complaint Status Endpoint
    @PutMapping("/complaints/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable("id") String id, @RequestBody String status) {
        Optional<Complaint> optionalComplaint = repository.findById(id);
        if (optionalComplaint.isPresent()) {
            Complaint complaint = optionalComplaint.get();
            String oldStatus = complaint.getStatus();
            String cleanStatus = status.replace("\"", "").trim();

            complaint.setStatus(cleanStatus);
            repository.save(complaint);

            historyRepository.save(new ComplaintHistory(id, oldStatus, cleanStatus));
            return ResponseEntity.ok(complaint);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Complaint not found");
    }

    // 5. Admin District Escalations Endpoint
    @GetMapping("/admin/{district}/escalations")
    public ResponseEntity<List<Complaint>> getDistrictEscalations(@PathVariable("district") String district) {
        List<Complaint> list = repository.findByDistrictIgnoreCaseAndStatusIgnoreCase(district.trim(), "ESCALATED");
        return ResponseEntity.ok(list);
    }

    // 6. Delete Complaint Endpoint
    @DeleteMapping("/complaints/{id}")
    public ResponseEntity<?> deleteComplaint(@PathVariable("id") String id) {
        Optional<Complaint> complaint = repository.findById(id);
        if (complaint.isPresent()) {
            deletePhotoFile(complaint.get().getPhotoPath());
            repository.deleteById(id);
            return ResponseEntity.ok("Complaint deleted successfully");
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Complaint not found with ID: " + id);
    }

    private int getSlaHoursForCategory(String category) {
        if ("WATER_SUPPLY".equalsIgnoreCase(category)) return 24;
        if ("ELECTRICITY".equalsIgnoreCase(category)) return 48;
        if ("ROADS".equalsIgnoreCase(category)) return 72;
        return 24;
    }

    private String saveUploadedFile(MultipartFile file) {
        try {
            String uploadDir = System.getProperty("user.dir") + "/uploads/";
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();

            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            File dest = new File(uploadDir + fileName);
            file.transferTo(dest);
            return fileName;
        } catch (IOException e) {
            return "default.jpg";
        }
    }

    private void deletePhotoFile(String photoPath) {
        if (photoPath != null && !photoPath.equals("default.jpg")) {
            String cleanName = photoPath.substring(photoPath.lastIndexOf('/') + 1)
                                        .substring(photoPath.lastIndexOf('\\') + 1);
            File file = new File(System.getProperty("user.dir") + "/uploads/" + cleanName);
            if (file.exists()) {
                file.delete();
            }
        }
    }
}