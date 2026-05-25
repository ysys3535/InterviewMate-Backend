package com.capstone.interviewmate.feedback.entity;

import com.capstone.interviewmate.session.entity.Session;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "feedback")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feedback_id")
    private Long feedbackId;

    @Column(name = "feedback_json", columnDefinition = "TEXT")
    private String feedbackJson;

    @ManyToOne
    @JoinColumn(name = "session_id")
    private Session session;
}