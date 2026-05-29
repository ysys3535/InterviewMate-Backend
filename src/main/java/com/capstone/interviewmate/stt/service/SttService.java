package com.capstone.interviewmate.stt.service;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@Slf4j
public class SttService {

    @Value("${openai.api-key}")
    private String apiKey;

    private final OkHttpClient client = new OkHttpClient();

    public String convertSpeechToText(MultipartFile audio) {
        if (audio == null || audio.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "음성 파일이 비어 있습니다.");
        }

        String contentType = audio.getContentType() != null ? audio.getContentType() : "application/octet-stream";
        String fileName = audio.getOriginalFilename() != null ? audio.getOriginalFilename() : "audio.webm";
        MediaType mediaType = MediaType.parse(contentType);

        try {
            RequestBody fileBody =
                    RequestBody.create(
                            audio.getBytes(),
                            mediaType
                    );

            MultipartBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                            "file",
                            fileName,
                            fileBody
                    )
                    .addFormDataPart("model", "whisper-1")
                    .build();

            Request request = new Request.Builder()
                    .url("https://api.openai.com/v1/audio/transcriptions")
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .post(requestBody)
                    .build();

            log.info(
                    "STT request received. filename={}, contentType={}, size={} bytes",
                    fileName,
                    contentType,
                    audio.getSize()
            );

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    log.error("OpenAI STT request failed. status={}, body={}", response.code(), responseBody);
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OpenAI STT 요청 실패");
                }

                JSONObject json = new JSONObject(responseBody);

                return json.getString("text");
            }
        } catch (IOException e) {
            log.error("OpenAI STT request error", e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "STT 변환 요청 실패");
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("STT response parsing failed", e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "STT 응답 처리 실패");
        }
    }
}
