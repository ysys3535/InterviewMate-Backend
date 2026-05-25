package com.capstone.interviewmate.global.gemini.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnalyzeRequest {

    private Long sessionId;
    private String answerText;
}