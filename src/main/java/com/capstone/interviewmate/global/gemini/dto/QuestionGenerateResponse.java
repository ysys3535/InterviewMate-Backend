package com.capstone.interviewmate.global.gemini.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QuestionGenerateResponse {

    private Long sessionId;

    private String mode;

    private String stage;

    private Integer questionOrder;

    private String question;

    private String questionAudioBase64;

    private String questionAudioContentType;
}
