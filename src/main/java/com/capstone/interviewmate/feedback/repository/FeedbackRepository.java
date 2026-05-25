package com.capstone.interviewmate.feedback.repository;

import com.capstone.interviewmate.feedback.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    Optional<Feedback> findTopBySessionSessionIdOrderByFeedbackIdDesc(Long sessionId);
}