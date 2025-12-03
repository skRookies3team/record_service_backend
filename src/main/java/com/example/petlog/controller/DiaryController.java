package com.example.petlog.controller;

import com.example.petlog.dto.request.DiaryRequest;
import com.example.petlog.dto.response.DiaryResponse;
import com.example.petlog.service.DiaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/diaries")
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryService diaryService;

//    // 생성 (POST)
//    @PostMapping
//    public ResponseEntity<Void> createDiary(@Valid @RequestBody DiaryRequest.Create request) {
//        Long diaryId = diaryService.createDiary(request);
//        return ResponseEntity.created(URI.create("/api/diaries/" + diaryId)).build();
//    }

    // 생성 (POST)
    @PostMapping
    public ResponseEntity<Map<String, Object>> createDiary(@Valid @RequestBody DiaryRequest.Create request) {
        // 1. 서비스 호출 및 ID 반환
        Long diaryId = diaryService.createDiary(request);

        // 2. 응답 메시지 커스텀 (Map 사용)
        Map<String, Object> response = new HashMap<>();
        response.put("diaryId", diaryId);
        response.put("message", "일기가 성공적으로 등록되었습니다.");

        // 3. 201 Created 상태코드 + Location 헤더 + Body 반환
        return ResponseEntity
                .created(URI.create("/api/diaries/" + diaryId))
                .body(response);
    }

    // 조회 (GET)
    @GetMapping("/{diaryId}")
    public ResponseEntity<DiaryResponse> getDiary(@PathVariable Long diaryId) {
        return ResponseEntity.ok(diaryService.getDiary(diaryId));
    }

    // 수정 (PATCH)
    @PatchMapping("/{diaryId}")
    public ResponseEntity<Void> updateDiary(@PathVariable Long diaryId,
                                            @RequestBody DiaryRequest.Update request) {
        diaryService.updateDiary(diaryId, request);
        return ResponseEntity.noContent().build();
    }

    // 삭제 (DELETE)
    @DeleteMapping("/{diaryId}")
    public ResponseEntity<Void> deleteDiary(@PathVariable Long diaryId) {
        diaryService.deleteDiary(diaryId);
        return ResponseEntity.noContent().build();
    }
}