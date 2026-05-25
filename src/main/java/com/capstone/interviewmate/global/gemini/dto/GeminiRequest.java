package com.capstone.interviewmate.global.gemini.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class GeminiRequest {

    private List<Content> contents;

    @Getter
    @AllArgsConstructor
    public static class Content {
        private List<Part> parts;
    }

    @Getter
    @AllArgsConstructor
    public static class Part {
        private String text;
    }
}