package com.capstone.interviewmate.feedback.controller;

import com.capstone.interviewmate.feedback.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @GetMapping("/{sessionId}")
    public String getFeedback(
            @PathVariable Long sessionId
    ) {
        return feedbackService.getFeedback(sessionId);
    }
}