package com.governance.repository;

import com.governance.model.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, String> {

    // 1. 특정 தொகுதியின் துறை சார்ந்த புகார்களை பெற (Case-Insensitive & Severity High to Low)
    List<Complaint> findByConstituencyIgnoreCaseAndCategoryIgnoreCaseOrderBySeverityScoreDesc(String constituency, String category);

    // 2. குறிப்பிட்ட தொகுதியின் அனைத்துப் புகார்களையும் பெற (Case-Insensitive & Severity High to Low)
    List<Complaint> findByConstituencyIgnoreCaseOrderBySeverityScoreDesc(String constituency);

    // 3. மாவட்ட ஆட்சியருக்கான குறிப்பிட்ட மாவட்டத்தின் ESCALATED புகார்கள்
    List<Complaint> findByDistrictIgnoreCaseAndStatusIgnoreCase(String district, String status);

    // 4. குறிப்பிட்ட மாவட்டத்தின் துறை சார்ந்த புகார்கள்
    List<Complaint> findByDistrictIgnoreCaseAndCategoryIgnoreCaseOrderBySeverityScoreDesc(String district, String category);

    // 5. தொகுதி, கிராமம், வகை மற்றும் இருப்பிடத்தின் மூலம் நகல் புகாரைக் கண்டறிதல்
    Optional<Complaint> findByConstituencyAndVillageAndCategoryAndLocationAndStatusNot(
            String constituency, String village, String category, String location, String status);

    // 6. 30-Meter GPS duplicate check-க்காக Category மற்றும் Unresolved Status கொண்ட புகார்களை எடுக்க
    List<Complaint> findByCategoryIgnoreCaseAndStatusNotIgnoreCase(String category, String status);
}