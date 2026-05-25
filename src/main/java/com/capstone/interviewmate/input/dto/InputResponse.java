package com.capstone.interviewmate.input.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InputResponse {

    private Long inputId;
    private Long sessionId;
    private String userPrompt;
}