package com.governance.service;

import org.springframework.stereotype.Service;

@Service
public class SeverityService {
    public int calculateSeverity(String description, String category) {
        int score = 1;
        String descLower = description.toLowerCase();
        
        if (descLower.contains("fire") || descLower.contains("hazard") || descLower.contains("emergency") || descLower.contains("spark")) {
            score += 3;
        }
        if (descLower.contains("pipe burst") || descLower.contains("sewage") || descLower.contains("blocked")) {
            score += 2;
        }
        if ("ELECTRICITY".equalsIgnoreCase(category) || "WATER_SUPPLY".equalsIgnoreCase(category)) {
            score += 1;
        }
        return Math.min(score, 5);
    }
}