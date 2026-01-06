package com.petlog.record.controller;

import com.petlog.record.dto.response.DiaryResponse;
import com.petlog.record.service.DiaryQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * [기록 조회 서비스 컨트롤러]
 * 사용자의 다이어리 데이터를 캘린더 기반 또는 특정 조건(AI 생성 여부 등)에 따라
 * 필터링하여 제공하는 조회 전용 API 컨트롤러
 */
@Tag(name = "Diary Query API", description = "다이어리 조회(캘린더/보관함) API") // 컨트롤러 설명 추가
@RestController
@RequestMapping("/api/diary-queries")
@RequiredArgsConstructor
public class DiaryQueryController {

    private final DiaryQueryService diaryQueryService;

    /**
     * [캘린더 날짜별 다이어리 조회 API]
     * 특정 사용자가 선택한 날짜에 작성한 다이어리 목록을 반환
     * 서비스 내 캘린더 UI에서 특정 날짜의 점(dot)이나 리스트를 클릭했을 때 호출됨
     * @param userId 사용자 식별자
     * @param date 조회하고자 하는 특정 날짜 (ISO DATE 포맷: YYYY-MM-DD)
     */
    @Operation(summary = "캘린더 날짜별 조회", description = "특정 사용자의 특정 날짜에 작성된 다이어리 목록을 조회합니다.")
    @GetMapping("/calendar")
    public ResponseEntity<List<DiaryResponse>> getDiariesByDate(
            @RequestParam Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(diaryQueryService.getDiariesByDate(userId, date));
    }

    /**
     * [AI 생성 다이어리 보관함 조회 API]
     * 사용자가 AI 초안 기능을 이용하여 생성한 다이어리들만 모아서 조회
     * 'AI 기록 보관함'과 같이 특정 성격의 데이터를 모아보는 화면에서 사용됨
     * @param userId 사용자 식별자
     */
    @Operation(summary = "AI 다이어리 보관함 조회", description = "특정 사용자가 AI를 통해 생성한 다이어리 목록(isAiGen=true)을 조회합니다.")
    @GetMapping("/ai-archive")
    public ResponseEntity<List<DiaryResponse>> getAiDiaries(@RequestParam Long userId) {
        return ResponseEntity.ok(diaryQueryService.getAiDiaries(userId));
    }
}