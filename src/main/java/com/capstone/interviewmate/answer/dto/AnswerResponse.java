package com.capstone.interviewmate.answer.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AnswerResponse {

    private Long answerId;

    private String questionText;

    private String answerText;

    private Integer answerDuration;

    private String feedback;
}