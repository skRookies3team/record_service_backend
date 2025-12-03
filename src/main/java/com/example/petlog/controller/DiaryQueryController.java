package com.example.petlog.controller;

import com.example.petlog.dto.response.DiaryResponse;
import com.example.petlog.service.DiaryQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/diary-queries")
@RequiredArgsConstructor
public class DiaryQueryController {

    private final DiaryQueryService diaryQueryService;

    // === 캘린더 날짜별 조회 API ===
    // GET /api/diaries/calendar?userId=1&date=2024-03-01
    @GetMapping("/calendar")
    public ResponseEntity<List<DiaryResponse>> getDiariesByDate(
            @RequestParam Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(diaryQueryService.getDiariesByDate(userId, date));
    }

    // === AI 다이어리 보관함 조회 API ===
    // GET /api/diaries/ai-archive?userId=1
    @GetMapping("/ai-archive")
    public ResponseEntity<List<DiaryResponse>> getAiDiaries(@RequestParam Long userId) {
        return ResponseEntity.ok(diaryQueryService.getAiDiaries(userId));
    }
}