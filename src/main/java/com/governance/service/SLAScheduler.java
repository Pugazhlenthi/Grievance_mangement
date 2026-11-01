package com.governance.service;

import com.governance.model.Complaint;
import com.governance.model.ComplaintHistory;
import com.governance.repository.ComplaintHistoryRepository;
import com.governance.repository.ComplaintRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class SLAScheduler {

    @Autowired
    private ComplaintRepository repository;

    @Autowired
    private ComplaintHistoryRepository historyRepository;

    @Scheduled(fixedRate = 60000) // Checks every 1 minute
    public void checkSLABreaches() {
        List<Complaint> activeComplaints = repository.findAll();
        LocalDateTime now = LocalDateTime.now();

        for (Complaint complaint : activeComplaints) {
            String status = complaint.getStatus();
            
            // Escalates complaints in 'REGISTERED' or 'IN_PROGRESS' status if deadline passes
            if (("REGISTERED".equals(status) || "IN_PROGRESS".equals(status)) 
                    && complaint.getSlaDeadline() != null 
                    && now.isAfter(complaint.getSlaDeadline())) {
                
                String oldStatus = complaint.getStatus();
                complaint.setStatus("ESCALATED");
                repository.save(complaint);

                historyRepository.save(new ComplaintHistory(complaint.getComplaintId(), oldStatus, "ESCALATED"));
            }
        }
    }
}