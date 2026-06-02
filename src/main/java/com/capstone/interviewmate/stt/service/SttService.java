package com.capstone.interviewmate.stt.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class SttService {

    @Value("${elevenlabs.api-key}")
    private String elevenLabsApiKey;

    private final WebClient webClient = WebClient.builder().build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String transcribe(MultipartFile audio) {
        if (audio == null || audio.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "음성 파일이 비어 있습니다.");
        }

        try {
            log.info(
                    "STT request received. filename={}, contentType={}, size={} bytes",
                    audio.getOriginalFilename(),
                    audio.getContentType(),
                    audio.getSize()
            );

            String responseBody = webClient.post()
                    .uri("https://api.elevenlabs.io/v1/speech-to-text")
                    .header("xi-api-key", elevenLabsApiKey)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData("model_id", "scribe_v2")
                            .with("file", audio.getResource()))
                    .retrieve()
                    .onStatus(
                            status -> status.isError(),
                            response -> response.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .map(body -> {
                                        log.error(
                                                "ElevenLabs STT request failed. status={}, body={}",
                                                response.statusCode(),
                                                body
                                        );
                                        return new ResponseStatusException(
                                                HttpStatus.BAD_GATEWAY,
                                                "ElevenLabs STT 요청 실패"
                                        );
                                    })
                    )
                    .bodyToMono(String.class)
                    .block();

            log.info("ElevenLabs STT raw response={}", responseBody);

            JsonNode json = objectMapper.readTree(responseBody);
            JsonNode text = json.get("text");
            if (text == null || text.asText().isBlank()) {
                log.error("ElevenLabs STT response missing text. response={}", json);
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "ElevenLabs STT 응답에 text가 없습니다."
                );
            }

            return text.asText();
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("ElevenLabs STT request failed", e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "ElevenLabs STT 요청 실패");
        }
    }
}
