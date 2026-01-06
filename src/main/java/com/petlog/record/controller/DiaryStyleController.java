package com.petlog.record.controller;

import com.petlog.record.dto.request.DiaryStyleRequest;
import com.petlog.record.dto.response.DiaryStyleResponse;
import com.petlog.record.service.DiaryStyleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * [다이어리 스타일 설정 컨트롤러]
 * 다이어리의 폰트, 테마 색상, 배경 등 사용자의 UI 커스터마이징 설정을 관리하는 API
 */
@Tag(name = "Diary Style API", description = "다이어리 스타일 설정(폰트, 테마 등) 관리 API")
@RestController
@RequestMapping("/api/v1/diary/styles")
@RequiredArgsConstructor
public class DiaryStyleController {

    private final DiaryStyleService diaryStyleService;

    /**
     * [스타일 설정 저장 및 갱신 API]
     * 사용자의 스타일 설정을 저장하며, 이미 설정값이 존재하는 경우 새로운 데이터로 업데이트(Upsert) 수행
     * @param userId 헤더(X-USER-ID)를 통해 전달받은 사용자 식별자
     * @param request 저장할 스타일 정보(폰트, 테마 등)
     */
    @Operation(summary = "스타일 설정 생성/수정", description = "사용자 또는 특정 펫의 다이어리 스타일 설정을 저장합니다. (기존 설정이 있으면 업데이트)")
    @PostMapping
    public ResponseEntity<DiaryStyleResponse> createStyle(
            @RequestBody DiaryStyleRequest request,
            @RequestHeader("X-USER-ID") Long userId
    ) {
        DiaryStyleResponse response = diaryStyleService.createOrUpdateStyle(userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * [스타일 상세 수정 API]
     * 특정 스타일 ID를 지정하여 해당 설정을 수정
     */
    @Operation(summary = "스타일 설정 수정", description = "특정 스타일 ID에 대한 설정을 수정합니다.")
    @PutMapping("/{styleId}")
    public ResponseEntity<DiaryStyleResponse> updateStyle(
            @PathVariable Long styleId,
            @RequestBody DiaryStyleRequest request,
            @RequestHeader("X-USER-ID") Long userId
    ) {
        DiaryStyleResponse response = diaryStyleService.updateStyle(styleId, request, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * [내 스타일 설정 조회 API]
     * 현재 로그인한 사용자의 기본 스타일 혹은 특정 펫에게 적용된 스타일을 조회
     * @param petId 특정 펫의 스타일을 조회하고 싶은 경우 포함 (Optional)
     */
    @Operation(summary = "내 스타일 조회", description = "사용자의 현재 스타일 설정을 조회합니다. (펫 ID 파라미터 선택 가능)")
    @GetMapping("/me")
    public ResponseEntity<DiaryStyleResponse> getMyStyle(
            @RequestHeader("X-USER-ID") Long userId,
            @RequestParam(required = false) Long petId
    ) {
        DiaryStyleResponse response = diaryStyleService.getUserStyle(userId, petId);
        return ResponseEntity.ok(response);
    }

    /**
     * [펫 전용 스타일 조회 API]
     * 반려동물별로 다르게 설정된 테마 정보를 조회
     */
    @Operation(summary = "펫 스타일 조회", description = "특정 펫의 스타일 설정을 조회합니다.")
    @GetMapping("/pet/{petId}")
    public ResponseEntity<DiaryStyleResponse> getPetStyle(
            @PathVariable Long petId,
            @RequestHeader("X-USER-ID") Long userId
    ) {
        DiaryStyleResponse response = diaryStyleService.getPetStyle(petId, userId);
        return ResponseEntity.ok(response);
    }
}