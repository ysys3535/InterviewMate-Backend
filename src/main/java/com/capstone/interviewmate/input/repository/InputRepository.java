package com.capstone.interviewmate.input.repository;

import com.capstone.interviewmate.input.entity.Input;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InputRepository extends JpaRepository<Input, Long> {
}