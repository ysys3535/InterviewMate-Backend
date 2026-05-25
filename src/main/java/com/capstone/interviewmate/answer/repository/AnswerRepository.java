package com.capstone.interviewmate.answer.repository;

import com.capstone.interviewmate.answer.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnswerRepository extends JpaRepository<Answer, Long> {
}