package com.capstone.interviewmate.input.controller;

import com.capstone.interviewmate.input.dto.InputCreateRequest;
import com.capstone.interviewmate.input.dto.InputResponse;
import com.capstone.interviewmate.input.service.InputService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inputs")
@RequiredArgsConstructor
public class InputController {

    private final InputService inputService;

    @PostMapping
    public InputResponse createInput(
            @RequestBody InputCreateRequest request
    ) {
        InputResponse input = inputService.createInput(request);
        return input;
    }
}