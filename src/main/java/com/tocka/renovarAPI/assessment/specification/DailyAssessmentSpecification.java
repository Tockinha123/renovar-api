package com.tocka.renovarAPI.assessment.specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.tocka.renovarAPI.assessment.entities.DailyAssessment;
import com.tocka.renovarAPI.patient.Patient;

import jakarta.persistence.criteria.Predicate;

public class DailyAssessmentSpecification {

    public static Specification<DailyAssessment> withFilters(Patient patient, LocalDate fromDate, LocalDate toDate) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filter by patient
            predicates.add(criteriaBuilder.equal(root.get("patient"), patient));

            // Filter by date range
            if (fromDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("assessmentDate"), fromDate));
            }
            if (toDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("assessmentDate"), toDate));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
