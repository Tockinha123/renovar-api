package com.tocka.renovarAPI.assessment.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tocka.renovarAPI.assessment.entities.AssessmentQuestion;
import com.tocka.renovarAPI.assessment.entities.AssessmentType;

@Repository
public interface AssessmentQuestionRepository extends JpaRepository<AssessmentQuestion, UUID> {
    List<AssessmentQuestion> findByTypeAndActiveTrue(AssessmentType type);
}
