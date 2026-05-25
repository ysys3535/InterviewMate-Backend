package com.capstone.interviewmate.session.dto;

import com.capstone.interviewmate.session.entity.SessionStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SessionResponse {

    private Long sessionId;
    private String sessionUuid;
    private String mode;
    private SessionStatus status;
    private Integer totalQuestionCount;
}