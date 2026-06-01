package com.capstone.interviewmate.feedback.service;

import com.capstone.interviewmate.feedback.entity.Feedback;
import com.capstone.interviewmate.feedback.repository.FeedbackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;

    public String getFeedback(Long sessionId) {
        Feedback feedback = feedbackRepository.findTopBySessionSessionIdOrderByFeedbackIdDesc(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Feedback not found"));

        return feedback.getFeedbackJson();
    }
}
