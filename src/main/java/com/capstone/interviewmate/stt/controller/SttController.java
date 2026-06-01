package com.capstone.interviewmate.stt.controller;

import com.capstone.interviewmate.stt.dto.SttResponse;
import com.capstone.interviewmate.stt.service.SttService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/stt")
@RequiredArgsConstructor
public class SttController {

    private final SttService sttService;

    @PostMapping(consumes = "multipart/form-data")
    public SttResponse speechToText(
            @RequestParam("audio") MultipartFile audio
    ) {
        String text = sttService.transcribe(audio);

        return new SttResponse(text);
    }
}
