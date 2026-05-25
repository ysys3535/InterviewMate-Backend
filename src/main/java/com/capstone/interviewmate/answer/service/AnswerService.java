package com.capstone.interviewmate.answer.service;

import com.capstone.interviewmate.answer.dto.AnswerCreateRequest;
import com.capstone.interviewmate.answer.dto.AnswerResponse;
import com.capstone.interviewmate.answer.entity.Answer;
import com.capstone.interviewmate.answer.repository.AnswerRepository;
import com.capstone.interviewmate.global.gemini.service.GeminiService;
import com.capstone.interviewmate.session.entity.Session;
import com.capstone.interviewmate.session.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AnswerService {

    private final AnswerRepository answerRepository;
    private final SessionRepository sessionRepository;
    private final GeminiService geminiService;

    public AnswerResponse createAnswer(AnswerCreateRequest request) {

        Session session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new RuntimeException("Session not found"));

        Answer answer = Answer.builder()
                .questionText(request.getQuestionText())
                .answerText(request.getAnswerText())
                .answerDuration(request.getAnswerDuration())
                .submittedAt(LocalDateTime.now())
                .session(session)
                .build();

        Answer savedAnswer = answerRepository.save(answer);

        String feedback = geminiService.analyzeAnswer(
                session.getSessionId(),
                request.getAnswerText()
        );

        return AnswerResponse.builder()
                .answerId(savedAnswer.getAnswerId())
                .questionText(savedAnswer.getQuestionText())
                .answerText(savedAnswer.getAnswerText())
                .answerDuration(savedAnswer.getAnswerDuration())
                .feedback(feedback)
                .build();
    }
}