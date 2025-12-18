package com.petlog.record.controller;

import com.petlog.record.dto.request.DiaryRequest;
import com.petlog.record.dto.response.DiaryResponse;
import com.petlog.record.service.DiaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Tag(name = "Diary API", description = "다이어리 CRUD 및 관리 API")
@RestController
@RequestMapping("/api/diaries")
@RequiredArgsConstructor
// [중요] 프론트엔드(5173)에서의 요청 허용
@CrossOrigin(origins = "http://localhost:5173")
public class DiaryController {

    private final DiaryService diaryService;

    @Operation(summary = "AI 일기 생성", description = "이미지를 분석하여 AI가 일기를 작성합니다.")
    @PostMapping(value = "/ai", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> createAiDiary(
            @Parameter(description = "업로드할 이미지 파일", content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
            @RequestPart("image") MultipartFile image,

            @Parameter(description = "일기 생성 요청 데이터 (JSON)", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = DiaryRequest.Create.class)))
            @Valid @RequestPart("data") DiaryRequest.Create request
    ) {
        log.info("AI 일기 생성 요청 - UserId: {}, PetId: {}", request.getUserId(), request.getPetId());

        Long diaryId = diaryService.createAiDiary(
                request.getUserId(),
                request.getPetId(),
                image,
                request.getVisibility(),
                request.getLocationName(),
                request.getLatitude(),
                request.getLongitude(),
                request.getDate()
        );

        Map<String, Object> response = new HashMap<>();
        response.put("diaryId", diaryId);
        response.put("message", "AI 일기가 성공적으로 생성되었습니다.");

        return ResponseEntity
                .created(URI.create("/api/diaries/" + diaryId))
                .body(response);
    }

    @Operation(summary = "다이어리 상세 조회", description = "다이어리 ID를 통해 일기의 상세 내용을 조회합니다.")
    @GetMapping("/{diaryId}")
    public ResponseEntity<DiaryResponse> getDiary(@PathVariable Long diaryId) {
        log.info("GET Diary Request - ID: {}", diaryId);
        return ResponseEntity.ok(diaryService.getDiary(diaryId));
    }

    @Operation(summary = "다이어리 수정", description = "기존 일기의 내용(텍스트, 공개범위, 날씨, 기분)을 부분 수정합니다.")
    @PatchMapping("/{diaryId}")
    public ResponseEntity<Void> updateDiary(@PathVariable Long diaryId,
                                            @RequestBody DiaryRequest.Update request) {
        diaryService.updateDiary(diaryId, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "다이어리 삭제", description = "특정 일기를 삭제합니다.")
    @DeleteMapping("/{diaryId}")
    public ResponseEntity<Void> deleteDiary(@PathVariable Long diaryId) {
        diaryService.deleteDiary(diaryId);
        return ResponseEntity.noContent().build();
    }
}