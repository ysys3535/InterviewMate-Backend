package com.capstone.interviewmate.answer.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnswerCreateRequest {

    private Long sessionId;

    private String questionText;

    private String answerText;

    private Integer answerDuration;
}