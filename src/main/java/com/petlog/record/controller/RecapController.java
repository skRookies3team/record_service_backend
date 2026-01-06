package com.petlog.record.controller;

import com.petlog.record.dto.request.RecapRequest;
import com.petlog.record.dto.response.RecapResponse;
import com.petlog.record.repository.jpa.DiaryRepository;
import com.petlog.record.service.RecapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * [AI 월간 리캡 컨트롤러]
 * 반려동물의 일기 데이터를 분석하여 월별 요약 콘텐츠(Recap)를 자동/수동으로 생성하고 조회하는 API
 */
@Tag(name = "Recap API", description = "AI 월간 리캡 자동 생성 및 조회 API")
@RestController
@RequestMapping("/api/recaps")
@RequiredArgsConstructor
public class RecapController {

    private final RecapService recapService;
    private final DiaryRepository diaryRepository;

    /**
     * [모든 펫 AI 리캡 자동 예약 API]
     * 사용자가 보유한 모든 반려동물(일기 기록이 있는 대상)에 대해 차월 리캡을 'WAITING' 상태로 미리 예약
     * 시스템 스케줄러가 차월 초에 해당 데이터를 바탕으로 요약을 수행할 수 있도록 기초 데이터를 생성함
     * @param userId 사용자 식별자
     */
    @Operation(summary = "모든 펫 AI 리캡 자동 예약", description = "사용자가 키우는 모든 펫에 대해 다음 달 리캡을 WAITING 상태로 예약합니다.")
    @PostMapping("/schedule/auto")
    public ResponseEntity<Map<String, Object>> scheduleAutoRecapForAllPets(
            @RequestParam Long userId) {

        // 1. 해당 유저가 일기를 쓴 적이 있는 모든 펫 ID 조회
        List<Long> petIds = diaryRepository.findDistinctPetIdsByUserId(userId);

        if (petIds.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "일기 기록이 있는 펫이 없습니다."));
        }

        LocalDate now = LocalDate.now();
        LocalDate nextMonthStart = now.plusMonths(1).withDayOfMonth(1);
        LocalDate nextMonthEnd = nextMonthStart.withDayOfMonth(nextMonthStart.lengthOfMonth());

        List<Long> createdRecapIds = new ArrayList<>();

        // 2. 각 펫별로 WAITING 리캡 생성
        for (Long petId : petIds) {
            RecapRequest.Create request = RecapRequest.Create.builder()
                    .petId(petId)
                    .userId(userId)
                    .title("리캡 생성 예정")
                    .summary("다음 달 리캡이 자동으로 생성될 예정입니다.")
                    .periodStart(nextMonthStart)
                    .periodEnd(nextMonthEnd)
                    .imageUrls(List.of())
                    .momentCount(0)
                    .status("WAITING")
                    .build();

            createdRecapIds.add(recapService.createWaitingRecap(request));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("petCount", petIds.size());
        response.put("recapIds", createdRecapIds);
        response.put("message", "총 " + petIds.size() + "마리 펫의 다음 달 리캡 예약이 완료되었습니다.");

        return ResponseEntity.ok(response);
    }

    /**
     * [모든 펫 AI 리캡 수동 일괄 생성 API]
     * 사용자가 지정한 특정 기간의 일기들을 분석하여 즉시 AI 리캡 리포트를 생성
     * 일기 기록이 없는 펫은 생성을 건너뛰며, 성공한 펫들에 대해서만 결과 ID를 반환
     * @param request 유저 ID 및 분석 대상 기간(시작일, 종료일)
     */
    @Operation(summary = "모든 펫 AI 리캡 수동 생성 (기간 지정)", description = "지정한 기간에 대해 키우는 모든 펫의 리캡을 즉시 생성합니다.")
    @PostMapping("/generate/manual")
    public ResponseEntity<Map<String, Object>> generateManualRecapForAllPets(
            @Valid @RequestBody RecapRequest.GenerateAll request) {

        // 1. 해당 유저의 모든 펫 ID 조회
        List<Long> petIds = diaryRepository.findDistinctPetIdsByUserId(request.getUserId());

        if (petIds.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "일기 기록이 있는 펫이 없습니다."));
        }

        List<Long> createdRecapIds = new ArrayList<>();

        // 2. 각 펫별로 리캡 생성 시도
        for (Long petId : petIds) {
            try {
                RecapRequest.Generate genRequest = RecapRequest.Generate.builder()
                        .petId(petId)
                        .userId(request.getUserId())
                        .periodStart(request.getPeriodStart())
                        .periodEnd(request.getPeriodEnd())
                        .petName("우리 아이")
                        .build();

                createdRecapIds.add(recapService.createAiRecap(genRequest));
            } catch (Exception e) {
                // 특정 기간에 일기가 없는 펫은 건너뜁니다.
                continue;
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("userId", request.getUserId());
        response.put("createdCount", createdRecapIds.size());
        response.put("recapIds", createdRecapIds);
        response.put("message", "총 " + createdRecapIds.size() + "마리 펫의 리캡 생성이 완료되었습니다.");

        return ResponseEntity.ok(response);
    }

    /**
     * [리캡 상세 조회 API]
     * 생성된 리캡의 상세 내용(요약 텍스트, 주요 키워드, 선택된 이미지 등)을 조회
     * 보안을 위해 요청한 유저가 해당 리캡의 소유자인지 검증하는 로직을 포함
     * @param recapId 리캡 식별자
     * @param userId 검증을 위한 사용자 식별자
     */
    @Operation(summary = "리캡 상세 조회", description = "생성된 리캡의 상세 내용을 조회합니다. (본인 것만 조회 가능)")
    @GetMapping("/{recapId}")
    public ResponseEntity<RecapResponse.Detail> getRecap(
            @PathVariable Long recapId,
            @RequestParam Long userId)
            { // 쿼리 파라미터로 userId를 받아 서비스에 전달
        return ResponseEntity.ok(recapService.getRecap(recapId, userId));
    }

    /**
     * [사용자별 리캡 목록 조회 API]
     * 특정 사용자가 보유한 모든 반려동물의 리캡 목록을 최신순으로 조회
     */
    @Operation(summary = "사용자별 리캡 목록 조회", description = "특정 사용자가 보유한 모든 리캡 목록을 조회합니다.")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<RecapResponse.Simple>> getAllRecaps(@PathVariable Long userId) {
        return ResponseEntity.ok(recapService.getAllRecaps(userId));
    }

    /**
     * [펫별 리캡 목록 조회 API]
     * 특정 반려동물에게 생성된 리캡 히스토리(연도별/월별 요약 목록)를 조회
     */
    @Operation(summary = "펫별 리캡 목록 조회", description = "특정 펫의 리캡 역사(History)를 조회합니다.")
    @GetMapping("/pet/{petId}")
    public ResponseEntity<List<RecapResponse.Simple>> getRecapsByPet(@PathVariable Long petId) {
        return ResponseEntity.ok(recapService.getRecapsByPet(petId));
    }
}