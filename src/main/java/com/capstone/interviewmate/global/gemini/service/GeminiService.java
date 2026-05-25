package com.capstone.interviewmate.global.gemini.service;

import com.capstone.interviewmate.feedback.entity.Feedback;
import com.capstone.interviewmate.feedback.repository.FeedbackRepository;
import com.capstone.interviewmate.global.gemini.dto.GeminiRequest;
import com.capstone.interviewmate.global.gemini.dto.QuestionGenerateRequest;
import com.capstone.interviewmate.global.gemini.dto.QuestionGenerateResponse;
import com.capstone.interviewmate.session.entity.Session;
import com.capstone.interviewmate.session.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GeminiService {

    @Value("${gemini.api-key}")
    private String apiKey;

    private final FeedbackRepository feedbackRepository;
    private final SessionRepository sessionRepository;

    public String analyzeAnswer(Long sessionId, String answerText) {

        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        String prompt = """
                너는 면접 답변을 평가하는 AI 면접관이야.

                아래 사용자 면접 답변을 평가해서 반드시 JSON 형식으로만 응답해.
                설명 문장, 마크다운, 코드블록 없이 순수 JSON만 반환해.

                평가 기준:
                - totalScore: 전체 점수, 0~100 사이 숫자
                - oneLineReview: 핵심 한줄 평가
                - overallFeedback: 조금 더 긴 종합 평가
                - summary: 각 항목을 '우수', '보통', '부족' 중 하나로 평가
                - details: 각 항목별 100점 만점 점수와 한줄 피드백
                - strengths: 강점 3개
                - improvements: 개선할 점 3개

                평가 항목:
                - delivery: 전달력
                - structure: 내용 구성
                - confidence: 자신감
                - timeManagement: 시간 관리
                - logic: 논리성

                반드시 아래 JSON 구조를 지켜줘.

                {
                  "totalScore": 0,
                  "oneLineReview": "",
                  "overallFeedback": "",
                  "summary": {
                    "delivery": "",
                    "structure": "",
                    "confidence": "",
                    "timeManagement": "",
                    "logic": ""
                  },
                  "details": {
                    "delivery": {"score": 0, "feedback": ""},
                    "structure": {"score": 0, "feedback": ""},
                    "confidence": {"score": 0, "feedback": ""},
                    "timeManagement": {"score": 0, "feedback": ""},
                    "logic": {"score": 0, "feedback": ""}
                  },
                  "strengths": [],
                  "improvements": []
                }

                사용자 답변:
                """ + answerText;

        String result = callGemini(prompt);

        Feedback feedback = Feedback.builder()
                .feedbackJson(result)
                .session(session)
                .build();

        feedbackRepository.save(feedback);

        return result;
    }

    public QuestionGenerateResponse generateQuestion(QuestionGenerateRequest request) {

        Session session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new RuntimeException("Session not found"));

        String prompt = """
                너는 실제 면접관처럼 질문을 생성하는 AI 면접관이야.

                사용자는 면접 연습을 진행 중이야.
                아래 정보를 바탕으로 다음 면접 질문을 하나만 생성해.

                [면접 모드]
                %s

                [현재 단계]
                %s

                [질문 순서]
                %d

                [이전 답변]
                %s

                [사용자 추가 입력 정보]
                %s

                질문 생성 규칙:
                1. 질문은 반드시 하나만 작성해.
                2. 설명, 번호, 따옴표 없이 질문 문장만 작성해.
                3. 질문은 한국어로 작성해.
                4. 모든 모드의 첫 번째 질문은 반드시 자기소개 질문으로 시작해.
                5. 질문 순서가 1이면 자기소개 질문을 생성해.
                6. 질문 순서가 2이면 이전 자기소개 답변을 바탕으로 후속 질문을 생성해.
                7. 질문 순서가 3 이상이면 지원 직무, 경험, 강점, 이전 답변을 바탕으로 면접 질문을 생성해.

                모드별 난이도:
                - BASIC: 쉬운 표현으로 부담 없는 질문을 생성해.
                - COMMON: 일반적인 실제 면접 수준의 질문을 생성해.
                - ADVANCED: 이전 답변을 깊게 파고드는 꼬리질문, 구체적 검증 질문을 생성해.
                """.formatted(
                request.getMode(),
                request.getStage(),
                request.getQuestionOrder(),
                request.getPreviousAnswer() == null ? "없음" : request.getPreviousAnswer(),
                request.getUserInput() == null ? "없음" : request.getUserInput()
        );

        String question = callGemini(prompt);

        return QuestionGenerateResponse.builder()
                .sessionId(session.getSessionId())
                .mode(request.getMode())
                .stage(request.getStage())
                .questionOrder(request.getQuestionOrder())
                .question(question)
                .build();
    }

    private String callGemini(String prompt) {

        WebClient webClient = WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .defaultHeader("x-goog-api-key", apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        GeminiRequest request = new GeminiRequest(
                List.of(
                        new GeminiRequest.Content(
                                List.of(new GeminiRequest.Part(prompt))
                        )
                )
        );

        Map response = webClient.post()
                .uri("/models/gemini-2.5-flash:generateContent")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        List candidates = (List) response.get("candidates");
        Map candidate = (Map) candidates.get(0);
        Map content = (Map) candidate.get("content");
        List parts = (List) content.get("parts");
        Map part = (Map) parts.get(0);

        return part.get("text").toString();
    }
}