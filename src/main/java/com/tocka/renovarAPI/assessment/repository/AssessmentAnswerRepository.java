package com.tocka.renovarAPI.assessment.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tocka.renovarAPI.assessment.entities.AssessmentAnswer;
import com.tocka.renovarAPI.assessment.entities.DailyAssessment;
import com.tocka.renovarAPI.assessment.entities.MonthlyAssessment;

@Repository
public interface AssessmentAnswerRepository extends JpaRepository<AssessmentAnswer, UUID> {
    List<AssessmentAnswer> findByDailyAssessment(DailyAssessment dailyAssessment);

    List<AssessmentAnswer> findByMonthlyAssessment(MonthlyAssessment monthlyAssessment);
}
