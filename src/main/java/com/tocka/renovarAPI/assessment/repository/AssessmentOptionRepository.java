package com.tocka.renovarAPI.assessment.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tocka.renovarAPI.assessment.entities.AssessmentOption;
import com.tocka.renovarAPI.assessment.entities.AssessmentQuestion;

@Repository
public interface AssessmentOptionRepository extends JpaRepository<AssessmentOption, UUID> {
    List<AssessmentOption> findByQuestionAndActiveTrue(AssessmentQuestion question);
}
