package com.capstone.interviewmate.global.gemini.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.capstone.interviewmate.feedback.entity.Feedback;
import com.capstone.interviewmate.feedback.repository.FeedbackRepository;
import com.capstone.interviewmate.global.gemini.dto.GeminiRequest;
import com.capstone.interviewmate.global.gemini.dto.QuestionGenerateRequest;
import com.capstone.interviewmate.global.gemini.dto.QuestionGenerateResponse;
import com.capstone.interviewmate.session.entity.Session;
import com.capstone.interviewmate.session.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.model}")
    private String geminiModel;

    private final FeedbackRepository feedbackRepository;
    private final SessionRepository sessionRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String analyzeAnswer(Long sessionId, String answerText) {
        if (sessionId == null || answerText == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sessionId와 answerText는 필수입니다.");
        }

        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

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
        if (request == null || request.getSessionId() == null || request.getQuestionOrder() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sessionId와 questionOrder는 필수입니다.");
        }

        Session session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        String mode = session.getMode() == null ? "" : session.getMode().trim().toUpperCase();
        if ("BASIC".equals(mode)) {
            if (request.getQuestionOrder() != 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "BASIC 모드는 자기소개 질문만 진행합니다.");
            }

            return QuestionGenerateResponse.builder()
                    .sessionId(session.getSessionId())
                    .mode(mode)
                    .stage(request.getStage())
                    .questionOrder(request.getQuestionOrder())
                    .question("1분 자기소개를 해주세요.")
                    .build();
        }
        if (!"COMMON".equals(mode) && !"ADVANCED".equals(mode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 면접 모드입니다.");
        }
        if (request.getQuestionOrder() < 1 || request.getQuestionOrder() > session.getTotalQuestionCount()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "질문 순서가 세션의 질문 수 범위를 벗어났습니다.");
        }

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

                [전체 질문 수]
                %d

                [이전 답변]
                %s

                [사용자 추가 입력 정보]
                %s

                질문 생성 규칙:
                1. 질문은 반드시 하나만 작성해.
                2. 설명, 번호, 따옴표 없이 질문 문장만 작성해.
                3. 질문은 한국어로 작성해.
                4. 첫 번째 질문은 반드시 자기소개 질문으로 시작해.
                5. 질문 순서가 1이면 자기소개 질문만 생성해.
                6. 질문 순서가 2 이상이면 이전 답변을 바탕으로 자연스럽게 이어지는 후속 질문을 생성해.
                7. 사용자 추가 입력 정보가 있으면 지원 직무, 경험, 강점, 기술 스택을 질문에 반영해.

                모드별 질문 구성:
                - COMMON: 총 5개 질문으로 진행하며, 일반적인 실제 면접 수준의 질문을 생성해.
                  자기소개 이후에는 경험, 직무 적합성, 협업, 문제 해결 역량을 균형 있게 확인해.
                - ADVANCED: 총 7개 질문으로 진행하며, COMMON보다 더 심화된 질문을 생성해.
                  자기소개 이후에는 이전 답변의 근거, 의사결정 과정, 실패 경험, 기술적 깊이, 구체적 성과를 파고드는 꼬리질문을 생성해.
                """.formatted(
                mode,
                request.getStage(),
                request.getQuestionOrder(),
                session.getTotalQuestionCount(),
                request.getPreviousAnswer() == null ? "없음" : request.getPreviousAnswer(),
                request.getUserInput() == null ? "없음" : request.getUserInput()
        );

        String question = callGemini(prompt);

        return QuestionGenerateResponse.builder()
                .sessionId(session.getSessionId())
                .mode(mode)
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

        try {
            String responseBody = webClient.post()
                    .uri("/models/{model}:generateContent", geminiModel)
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(
                            status -> status.isError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .map(body -> {
                                        log.error(
                                                "Gemini request failed. model={}, status={}, body={}",
                                                geminiModel,
                                                clientResponse.statusCode(),
                                                body
                                        );
                                        return new ResponseStatusException(
                                                HttpStatus.BAD_GATEWAY,
                                                "Gemini 요청 실패"
                                        );
                                    })
                    )
                    .bodyToMono(String.class)
                    .retryWhen(Retry.backoff(2, Duration.ofSeconds(1))
                            .filter(this::isRetryableGeminiError)
                            .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> retrySignal.failure()))
                    .block();

            log.info("Gemini raw response={}", responseBody);

            JsonNode response = objectMapper.readTree(responseBody);
            JsonNode text = response == null
                    ? null
                    : response.path("candidates").path(0).path("content").path("parts").path(0).get("text");

            if (text == null || text.asText().isBlank()) {
                log.error("Gemini response missing text. response={}", response);
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Gemini 응답에 text가 없습니다."
                );
            }

            return text.asText();
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Gemini response processing failed", e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gemini 응답 처리 실패");
        }
    }

    private boolean isRetryableGeminiError(Throwable throwable) {
        if (!(throwable instanceof ResponseStatusException exception)) {
            return false;
        }

        return exception.getStatusCode().is5xxServerError();
    }
}
