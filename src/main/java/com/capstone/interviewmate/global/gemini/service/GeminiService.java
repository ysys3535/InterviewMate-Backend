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
import com.capstone.interviewmate.tts.service.TtsService;
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
import java.util.Base64;
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
    private final TtsService ttsService;
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

            return buildQuestionResponse(session, mode, request, createBasicQuestion(request));
        }
        if (!"COMMON".equals(mode) && !"ADVANCED".equals(mode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 면접 모드입니다.");
        }
        if (request.getQuestionOrder() < 1 || request.getQuestionOrder() > session.getTotalQuestionCount()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "질문 순서가 세션의 질문 수 범위를 벗어났습니다.");
        }
        if (request.getQuestionOrder() == 1) {
            return buildQuestionResponse(session, mode, request, createBasicQuestion(request));
        }
        if (request.getQuestionOrder().equals(session.getTotalQuestionCount())) {
            return buildQuestionResponse(session, mode, request, createClosingQuestion(request));
        }

        String prompt = """
                너는 현재 최고의 대기업 인사담당자이자 실무 면접관이다.
                아래 제공된 입력 정보와 일반적인 기업/직무 상식을 바탕으로 질문을 생성한다.

                [목표 기업]
                %s

                [목표 직무]
                %s

                [면접 모드]
                %s

                [질문 스타일]
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

                면접관 페르소나:
                - 실제 채용 면접처럼 지원자의 역량, 판단 근거, 실무 적합성을 검증한다.
                - 목표 기업이 있으면 그 기업의 일반적으로 알려진 사업 특성, 제품 특성, 고객 경험, 안정성, 보안, 확장성 관점을 반영한다.
                - 목표 직무가 있으면 해당 직무의 실무 역량, 협업 방식, 문제 해결 방식, 기술적 깊이를 검증한다.
                - 압박형 질문은 가능하지만 무례하거나 공격적인 표현은 쓰지 않는다.

                질문 생성 규칙:
                1. 질문은 반드시 하나만 작성한다.
                2. 설명, 번호, 따옴표, 마크다운 없이 질문 문장만 반환한다.
                3. 질문은 한국어로 작성한다.
                4. 질문 순서 2~6은 이전 답변에서 나온 주장, 경험, 기술 선택, 성과, 실패, 의사결정 근거를 파고드는 후속질문으로 생성한다.
                5. 첫 번째 자기소개 질문과 마지막 기타 인터뷰 질문은 이미 별도로 처리되므로 생성하지 않는다.
                6. 사용자 추가 입력 정보에 기술스택, 프로젝트, 경험, 강점, 희망 직무가 있으면 반드시 질문에 반영한다.
                7. 목표 기업/직무 정보가 부족하면 일반 질문으로 후퇴하지 말고, 입력된 정보 안에서 가장 구체적인 실무 질문을 만든다.
                8. 사실 확인이 필요한 최신 이슈, 실제 면접 후기, 특정 전형 내용은 지어내지 않는다.

                질문 스타일별 기준:
                - GENERAL: 기본적인 실무 역량과 경험을 확인한다.
                - PRACTICAL: 실제 업무 상황, 장애 대응, 협업, 우선순위 판단을 묻는다.
                - PRESSURE: 답변의 근거와 한계를 날카롭게 확인하되 예의 있게 묻는다.
                - FOLLOW_UP: 이전 답변의 모호한 부분을 구체적으로 검증하는 꼬리질문을 만든다.

                모드별 질문 구성:
                - COMMON: 총 7개 질문으로 진행한다. 1번째는 자기소개, 2~6번째는 후속질문 5개, 7번째는 기타 인터뷰로 하고 싶은 말을 묻는다.
                - ADVANCED: 총 7개 질문으로 진행한다. 1번째는 자기소개, 2~6번째는 더 깊은 후속질문 5개, 7번째는 기타 인터뷰로 하고 싶은 말을 묻는다.

                출력:
                면접 질문 한 문장만 출력한다.
                """.formatted(
                normalizePromptValue(request.getCompanyName()),
                normalizePromptValue(request.getJobRole()),
                mode,
                normalizeQuestionStyle(request.getQuestionStyle(), request.getQuestionOrder()),
                request.getStage(),
                request.getQuestionOrder(),
                session.getTotalQuestionCount(),
                normalizePromptValue(request.getPreviousAnswer()),
                normalizePromptValue(request.getUserInput())
        );

        String question = callGemini(prompt);

        return buildQuestionResponse(session, mode, request, question);
    }

    private QuestionGenerateResponse buildQuestionResponse(
            Session session,
            String mode,
            QuestionGenerateRequest request,
            String question
    ) {
        String audioBase64 = Base64.getEncoder().encodeToString(ttsService.synthesize(question));

        return QuestionGenerateResponse.builder()
                .sessionId(session.getSessionId())
                .mode(mode)
                .stage(request.getStage())
                .questionOrder(request.getQuestionOrder())
                .question(question)
                .questionAudioBase64(audioBase64)
                .questionAudioContentType(TtsService.AUDIO_CONTENT_TYPE)
                .build();
    }

    private String createBasicQuestion(QuestionGenerateRequest request) {
        String companyName = normalizePromptValue(request.getCompanyName());
        String jobRole = normalizePromptValue(request.getJobRole());

        if (!"없음".equals(companyName) && !"없음".equals(jobRole)) {
            return "%s %s 직무에 지원한 이유와 본인의 핵심 강점을 중심으로 1분 자기소개를 해주세요."
                    .formatted(companyName, jobRole);
        }
        if (!"없음".equals(jobRole)) {
            return "%s 직무에 지원한 이유와 본인의 핵심 강점을 중심으로 1분 자기소개를 해주세요."
                    .formatted(jobRole);
        }
        if (!"없음".equals(companyName)) {
            return "%s에 지원한 이유와 본인의 핵심 강점을 중심으로 1분 자기소개를 해주세요."
                    .formatted(companyName);
        }

        return "지원 직무와 본인의 핵심 강점을 중심으로 1분 자기소개를 해주세요.";
    }

    private String createClosingQuestion(QuestionGenerateRequest request) {
        String companyName = normalizePromptValue(request.getCompanyName());
        String jobRole = normalizePromptValue(request.getJobRole());

        if (!"없음".equals(companyName) && !"없음".equals(jobRole)) {
            return "마지막으로 %s %s 직무와 관련해 추가로 어필하고 싶은 경험이나 하고 싶은 말이 있다면 말씀해주세요."
                    .formatted(companyName, jobRole);
        }
        if (!"없음".equals(jobRole)) {
            return "마지막으로 %s 직무와 관련해 추가로 어필하고 싶은 경험이나 하고 싶은 말이 있다면 말씀해주세요."
                    .formatted(jobRole);
        }
        if (!"없음".equals(companyName)) {
            return "마지막으로 %s 지원과 관련해 추가로 어필하고 싶은 경험이나 하고 싶은 말이 있다면 말씀해주세요."
                    .formatted(companyName);
        }

        return "마지막으로 추가로 어필하고 싶은 경험이나 하고 싶은 말이 있다면 말씀해주세요.";
    }

    private String normalizePromptValue(String value) {
        return value == null || value.isBlank() ? "없음" : value.trim();
    }

    private String normalizeQuestionStyle(String questionStyle, Integer questionOrder) {
        if (questionStyle == null || questionStyle.isBlank()) {
            return questionOrder != null && questionOrder > 1 ? "FOLLOW_UP" : "PRACTICAL";
        }

        return switch (questionStyle.trim().toUpperCase()) {
            case "GENERAL", "PRACTICAL", "PRESSURE", "FOLLOW_UP" -> questionStyle.trim().toUpperCase();
            default -> "PRACTICAL";
        };
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
