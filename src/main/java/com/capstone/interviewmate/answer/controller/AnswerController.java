package com.capstone.interviewmate.answer.controller;

import com.capstone.interviewmate.answer.dto.AnswerCreateRequest;
import com.capstone.interviewmate.answer.dto.AnswerResponse;
import com.capstone.interviewmate.answer.service.AnswerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/answers")
@RequiredArgsConstructor
public class AnswerController {

    private final AnswerService answerService;

    @PostMapping
    public AnswerResponse createAnswer(
            @RequestBody AnswerCreateRequest request
    ) {
        return answerService.createAnswer(request);
    }
}