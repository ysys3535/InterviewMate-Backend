package com.capstone.interviewmate.session.service;

import com.capstone.interviewmate.session.dto.SessionCreateRequest;
import com.capstone.interviewmate.session.dto.SessionResponse;
import com.capstone.interviewmate.session.entity.Session;
import com.capstone.interviewmate.session.entity.SessionStatus;
import com.capstone.interviewmate.session.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository sessionRepository;

    public SessionResponse createSession(SessionCreateRequest request) {
        if (request == null || request.getMode() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "mode는 필수입니다.");
        }

        String mode = request.getMode().trim().toUpperCase();
        int totalQuestionCount = getTotalQuestionCount(mode);

        Session session = Session.builder()
                .sessionUuid(UUID.randomUUID().toString())
                .mode(mode)
                .status(SessionStatus.IN_PROGRESS)
                .currentStage("QUESTION")
                .totalQuestionCount(totalQuestionCount)
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

    private int getTotalQuestionCount(String mode) {
        return switch (mode) {
            case "BASIC" -> 1;
            case "COMMON" -> 5;
            case "ADVANCED" -> 7;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 면접 모드입니다.");
        };
    }

    @Transactional
    public void endSession(Long sessionId) {

        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "세션을 찾을 수 없습니다."));

        session.setStatus(SessionStatus.COMPLETED);
        session.setEndedAt(LocalDateTime.now());
    }
}
