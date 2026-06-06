package com.capstone.interviewmate.tts.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class TtsService {

    public static final String AUDIO_CONTENT_TYPE = "audio/mpeg";

    @Value("${elevenlabs.api-key}")
    private String elevenLabsApiKey;

    @Value("${elevenlabs.voice-id}")
    private String voiceId;

    @Value("${elevenlabs.tts-model}")
    private String ttsModel;

    @Value("${elevenlabs.tts-output-format}")
    private String outputFormat;

    @Value("${elevenlabs.tts-language-code:}")
    private String languageCode;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.elevenlabs.io/v1")
            .build();

    public byte[] synthesize(String text) {
        if (text == null || text.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TTS 변환할 텍스트가 비어 있습니다.");
        }

        Map<String, Object> request = new HashMap<>();
        request.put("text", text);
        request.put("model_id", ttsModel);
        if (languageCode != null && !languageCode.isBlank()) {
            request.put("language_code", languageCode);
        }

        try {
            byte[] audio = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/text-to-speech/{voiceId}")
                            .queryParam("output_format", outputFormat)
                            .build(voiceId))
                    .header("xi-api-key", elevenLabsApiKey)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(
                            status -> status.isError(),
                            response -> response.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .map(body -> {
                                        log.error(
                                                "ElevenLabs TTS request failed. status={}, body={}",
                                                response.statusCode(),
                                                body
                                        );
                                        return new ResponseStatusException(
                                                HttpStatus.BAD_GATEWAY,
                                                "ElevenLabs TTS 요청 실패"
                                        );
                                    })
                    )
                    .bodyToMono(byte[].class)
                    .block();

            if (audio == null || audio.length == 0) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "ElevenLabs TTS 응답 오디오가 비어 있습니다.");
            }

            return audio;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("ElevenLabs TTS request failed", e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "ElevenLabs TTS 요청 실패");
        }
    }
}
