package com.capstone.interviewmate.input.entity;

import com.capstone.interviewmate.session.entity.Session;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inputs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Input {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "input_id")
    private Long inputId;

    @Column(name = "user_prompt", columnDefinition = "TEXT")
    private String userPrompt;

    @ManyToOne
    @JoinColumn(name = "session_id")
    private Session session;
}