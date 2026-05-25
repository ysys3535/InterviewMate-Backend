package com.capstone.interviewmate.session.controller;

import com.capstone.interviewmate.session.dto.SessionCreateRequest;
import com.capstone.interviewmate.session.dto.SessionResponse;
import com.capstone.interviewmate.session.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @PostMapping
    public SessionResponse createSession(
            @RequestBody SessionCreateRequest request
    ) {
        return sessionService.createSession(request);
    }

    @PatchMapping("/{sessionId}/end")
    public ResponseEntity<?> endSession(
            @PathVariable Long sessionId
    ) {
        sessionService.endSession(sessionId);

        return ResponseEntity.ok(Map.of(
                "message", "면접이 종료되었습니다.",
                "sessionId", sessionId,
                "status", "COMPLETED"
        ));
    }
}