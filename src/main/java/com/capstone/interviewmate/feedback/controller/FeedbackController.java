package com.capstone.interviewmate.feedback.controller;

import com.capstone.interviewmate.feedback.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @GetMapping("/{sessionId}")
    public ResponseEntity<String> getFeedback(
            @PathVariable Long sessionId
    ) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(feedbackService.getFeedback(sessionId));
    }
}
