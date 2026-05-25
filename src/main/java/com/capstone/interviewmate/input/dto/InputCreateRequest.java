package com.capstone.interviewmate.input.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InputCreateRequest {

    private Long sessionId;
    private String userPrompt;
}