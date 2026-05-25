package com.capstone.interviewmate.analysis.entity;

import com.capstone.interviewmate.answer.entity.Answer;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "analysis")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Analysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "analysis_id")
    private Long analysisId;

    @Column(name = "confidence_score")
    private Integer confidenceScore;

    @Column(name = "speed_score")
    private Integer speedScore;

    @Column(name = "logic_score")
    private Integer logicScore;

    @Column(name = "tone_score")
    private Integer toneScore;

    @Column(name = "keyword_score")
    private Integer keywordScore;

    @Column(name = "analysis_summary", columnDefinition = "TEXT")
    private String analysisSummary;

    @OneToOne
    @JoinColumn(name = "answer_id")
    private Answer answer;
}