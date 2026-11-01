package com.governance.repository;

import com.governance.model.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, String> {

    // குறிப்பிட்ட தொகுதியின் துறை சார்ந்த புகார்களைப் பெறுதல்
    List<Complaint> findByConstituencyAndCategoryOrderBySeverityScoreDesc(String constituency, String category);

    // குறிப்பிட்ட தொகுதியின் அனைத்துப் புகார்களையும் பெறுதல்
    List<Complaint> findByConstituencyOrderBySeverityScoreDesc(String constituency);

    // மாவட்ட ஆட்சியருக்கான குறிப்பிட்ட மாவட்டப் புகார்கள்
    List<Complaint> findByDistrictAndStatus(String district, String status);

    List<Complaint> findByDistrictAndCategoryOrderBySeverityScoreDesc(String district, String category);

    // தொகுதி, கிராமம், வகை மற்றும் இருப்பிடத்தின் மூலம் நகல் புகாரைக் கண்டறிதல்
    Optional<Complaint> findByConstituencyAndVillageAndCategoryAndLocationAndStatusNot(
            String constituency, String village, String category, String location, String status);
}