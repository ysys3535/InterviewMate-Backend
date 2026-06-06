package com.capstone.interviewmate.global.gemini.dto;

import lombok.Getter;

@Getter
public class QuestionGenerateRequest {

    private Long sessionId;

    private String mode;

    private String stage;

    private String previousAnswer;

    private String userInput;

    private String companyName;

    private String jobRole;

    private String questionStyle;

    private Integer questionOrder;
}
