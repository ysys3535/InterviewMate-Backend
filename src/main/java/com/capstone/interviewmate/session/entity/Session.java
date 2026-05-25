package com.capstone.interviewmate.session.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Long sessionId;

    @Column(name = "session_uuid")
    private String sessionUuid;

    private String mode;

    @Column(name = "current_stage")
    private String currentStage;

    @Enumerated(EnumType.STRING)
    private SessionStatus status;

    @Column(name = "total_question_count")
    private Integer totalQuestionCount;

    @Column(name = "current_question_order")
    private Integer currentQuestionOrder;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;
}