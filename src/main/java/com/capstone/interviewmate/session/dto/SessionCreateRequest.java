package com.capstone.interviewmate.session.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SessionCreateRequest {

    private String mode;
    private Integer totalQuestionCount;
}