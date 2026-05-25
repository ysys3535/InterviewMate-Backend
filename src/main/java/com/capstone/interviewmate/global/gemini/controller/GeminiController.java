package com.capstone.interviewmate.global.gemini.controller;

import com.capstone.interviewmate.global.gemini.dto.AnalyzeRequest;
import com.capstone.interviewmate.global.gemini.dto.QuestionGenerateRequest;
import com.capstone.interviewmate.global.gemini.dto.QuestionGenerateResponse;
import com.capstone.interviewmate.global.gemini.service.GeminiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/gemini")
@RequiredArgsConstructor
public class GeminiController {

    private final GeminiService geminiService;

    @PostMapping("/analyze")
    public String analyze(
            @RequestBody AnalyzeRequest request
    ) {
        return geminiService.analyzeAnswer(
                request.getSessionId(),
                request.getAnswerText()
        );
    }

    @PostMapping("/question")
    public QuestionGenerateResponse generateQuestion(
            @RequestBody QuestionGenerateRequest request
    ) {
        return geminiService.generateQuestion(request);
    }
}