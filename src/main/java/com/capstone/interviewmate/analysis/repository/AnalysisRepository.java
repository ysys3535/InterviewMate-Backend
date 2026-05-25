package com.capstone.interviewmate.analysis.repository;

import com.capstone.interviewmate.analysis.entity.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisRepository extends JpaRepository<Analysis, Long> {
}