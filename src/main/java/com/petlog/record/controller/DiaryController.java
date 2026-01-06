package com.petlog.record.controller;

import com.petlog.record.dto.request.DiaryRequest;
import com.petlog.record.dto.response.AiDiaryResponse;
import com.petlog.record.dto.response.DiaryResponse;
import com.petlog.record.service.DiaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;

/**
 * [기록 서비스 다이어리 컨트롤러]
 * 반려동물의 일상 기록(일기) 생성, 조회, 수정, 삭제 및 AI 기반 일기 초안 생성 기능을 제공
 */
@Slf4j
@Tag(name = "Diary API", description = "다이어리 CRUD 및 관리 API")
@RestController
@RequestMapping("/api/diaries")
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryService diaryService;


    // [AI 일기 미리보기 생성 API]
    // 이미지를 AI로 분석하여 일기 초안을 작성하고, 위치 정보 기반으로 당시의 날씨 정보를 함께 조회
    @Operation(summary = "AI 일기 미리보기 생성", description = "이미지를 분석하여 AI가 일기 초안을 작성합니다. (위치 정보 기반 날씨 조회 포함)")
    @PostMapping(value = "/ai/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AiDiaryResponse> previewAiDiary(
            @RequestPart(value = "imageFiles", required = false) List<MultipartFile> imageFiles,
            @RequestPart("userId") Long userId,
            @RequestPart("petId") Long petId,
            @RequestPart(value = "images", required = false) List<DiaryRequest.Image> images,

            // [NEW] 위치 및 날짜 정보 추가 (required = false로 하여 없는 경우도 대비)
            @RequestPart(value = "latitude", required = false) Double latitude,
            @RequestPart(value = "longitude", required = false) Double longitude,
            @RequestPart(value = "date", required = false) String date // LocalDate 파싱 필요 시 변환
    ) {
        log.info("AI 일기 미리보기 요청 - UserId: {}, PetId: {}, Lat: {}, Lng: {}", userId, petId, latitude, longitude);
        // 서비스 메서드 호출 시 위치 정보 전달
        AiDiaryResponse response = diaryService.previewAiDiary(userId, petId, images, imageFiles, latitude, longitude, date);
        return ResponseEntity.ok(response);
    }

    // [다이어리 최종 저장 API]
    // AI 초안을 바탕으로 사용자가 수정한 최종 일기 내용을 데이터베이스에 저장
    @Operation(summary = "다이어리 최종 저장", description = "사용자가 수정한 최종 내용을 바탕으로 일기를 DB에 저장합니다.")
    @PostMapping
    public ResponseEntity<Long> createDiary(
            @Valid @RequestBody DiaryRequest.Create request
    ) {
        log.info("일기 최종 저장 요청 - UserId: {}, PetId: {}", request.getUserId(), request.getPetId());

        Long diaryId = diaryService.saveDiary(request);

        return ResponseEntity
                .created(URI.create("/api/diaries/" + diaryId))
                .body(diaryId);
    }

    /**
     * [다이어리 상세 조회 API]
     * 특정 일기 ID에 해당하는 상세 정보(내용, 이미지, 반려동물 정보 등)를 조회
     */
    @Operation(summary = "다이어리 상세 조회", description = "다이어리 ID를 통해 일기의 상세 내용을 조회합니다.")
    @GetMapping("/{diaryId}")
    public ResponseEntity<DiaryResponse> getDiary(@PathVariable Long diaryId) {
        log.info("GET Diary Request - ID: {}", diaryId);
        return ResponseEntity.ok(diaryService.getDiary(diaryId));
    }

    /**
     * [다이어리 수정 API]
     * 기존 일기의 텍스트, 공개 범위, 기분 등 선택적 필드를 부분 수정(Patch)
     */
    @Operation(summary = "다이어리 수정", description = "기존 일기의 내용(텍스트, 공개범위, 날씨, 기분)을 부분 수정합니다.")
    @PatchMapping("/{diaryId}")
    public ResponseEntity<Void> updateDiary(@PathVariable Long diaryId,
                                            @Valid @RequestBody DiaryRequest.Update request) {
        log.info("PATCH Diary Request - ID: {}", diaryId);
        diaryService.updateDiary(diaryId, request);
        return ResponseEntity.noContent().build();
    }

    /**
     * [다이어리 삭제 API]
     * 특정 일기 데이터를 논리적 또는 물리적으로 삭제 처리
     */
    @Operation(summary = "다이어리 삭제", description = "특정 일기를 삭제합니다.")
    @DeleteMapping("/{diaryId}")
    public ResponseEntity<Void> deleteDiary(@PathVariable Long diaryId) {
        log.info("DELETE Diary Request - ID: {}", diaryId);
        diaryService.deleteDiary(diaryId);
        return ResponseEntity.noContent().build();
    }
}