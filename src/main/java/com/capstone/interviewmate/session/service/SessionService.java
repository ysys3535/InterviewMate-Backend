package com.capstone.interviewmate.session.service;

import com.capstone.interviewmate.session.dto.SessionCreateRequest;
import com.capstone.interviewmate.session.dto.SessionResponse;
import com.capstone.interviewmate.session.entity.Session;
import com.capstone.interviewmate.session.entity.SessionStatus;
import com.capstone.interviewmate.session.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository sessionRepository;

    public SessionResponse createSession(SessionCreateRequest request) {

        Session session = Session.builder()
                .sessionUuid(UUID.randomUUID().toString())
                .mode(request.getMode())
                .status(SessionStatus.IN_PROGRESS)
                .currentStage("QUESTION")
                .totalQuestionCount(request.getTotalQuestionCount())
                .currentQuestionOrder(1)
                .startedAt(LocalDateTime.now())
                .build();

        Session savedSession = sessionRepository.save(session);

        return SessionResponse.builder()
                .sessionId(savedSession.getSessionId())
                .sessionUuid(savedSession.getSessionUuid())
                .mode(savedSession.getMode())
                .status(savedSession.getStatus())
                .totalQuestionCount(savedSession.getTotalQuestionCount())
                .build();
    }

    @Transactional
    public void endSession(Long sessionId) {

        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("세션을 찾을 수 없습니다."));

        session.setStatus(SessionStatus.COMPLETED);
        session.setEndedAt(LocalDateTime.now());
    }
}