package com.capstone.interviewmate.stt.service;

import lombok.RequiredArgsConstructor;
import okhttp3.*;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class SttService {

    @Value("${openai.api-key}")
    private String apiKey;

    public String convertSpeechToText(MultipartFile audio) {

        OkHttpClient client = new OkHttpClient();

        try {

            RequestBody fileBody =
                    RequestBody.create(
                            audio.getBytes(),
                            MediaType.parse(audio.getContentType())
                    );

            MultipartBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                            "file",
                            audio.getOriginalFilename(),
                            fileBody
                    )
                    .addFormDataPart("model", "whisper-1")
                    .build();

            Request request = new Request.Builder()
                    .url("https://api.openai.com/v1/audio/transcriptions")
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .post(requestBody)
                    .build();

            Response response = client.newCall(request).execute();

            String responseBody = response.body().string();

            JSONObject json = new JSONObject(responseBody);

            return json.getString("text");

        } catch (IOException e) {
            throw new RuntimeException("STT 변환 실패");
        }
    }
}