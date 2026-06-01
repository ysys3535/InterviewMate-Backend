package com.capstone.interviewmate.input.service;

import com.capstone.interviewmate.input.dto.InputCreateRequest;
import com.capstone.interviewmate.input.dto.InputResponse;
import com.capstone.interviewmate.input.entity.Input;
import com.capstone.interviewmate.input.repository.InputRepository;
import com.capstone.interviewmate.session.entity.Session;
import com.capstone.interviewmate.session.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class InputService {

    private final InputRepository inputRepository;
    private final SessionRepository sessionRepository;

    public InputResponse createInput(InputCreateRequest request) {
        if (request == null || request.getSessionId() == null || request.getUserPrompt() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sessionId와 userPrompt는 필수입니다.");
        }

        Session session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        Input input = Input.builder()
                .userPrompt(request.getUserPrompt())
                .session(session)
                .build();

        Input savedInput = inputRepository.save(input);

        return InputResponse.builder()
                .inputId(savedInput.getInputId())
                .sessionId(session.getSessionId())
                .userPrompt(savedInput.getUserPrompt())
                .build();
    }
}
