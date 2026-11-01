package com.governance.service;

import com.governance.model.Complaint;
import com.governance.repository.ComplaintRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class DataCleanupScheduler {

    @Autowired
    private ComplaintRepository repository;

    // தினமும் நள்ளிரவு 12 மணிக்கு தானாக இயங்கும்
    @Scheduled(cron = "0 0 0 * * ?")
    public void autoDeleteOldComplaints() {
        LocalDateTime fiveYearsAgo = LocalDateTime.now().minusYears(5);
        List<Complaint> oldComplaints = repository.findAll();

        for (Complaint c : oldComplaints) {
            if (c.getCreatedAt() != null && c.getCreatedAt().isBefore(fiveYearsAgo)) {
                deletePhotoFile(c.getPhotoPath());
                repository.deleteById(c.getComplaintId());
                System.out.println("Auto-deleted 5-year-old complaint: " + c.getComplaintId());
            }
        }
    }

    public void deletePhotoFile(String photoPath) {
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