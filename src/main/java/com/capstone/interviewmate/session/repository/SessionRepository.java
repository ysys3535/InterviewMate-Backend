package com.capstone.interviewmate.session.repository;

import com.capstone.interviewmate.session.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<Session, Long> {
}