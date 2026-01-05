package com.petlog.record.controller;

import com.petlog.record.dto.request.RecapRequest;
import com.petlog.record.dto.response.RecapResponse;
import com.petlog.record.entity.Recap;
import com.petlog.record.entity.RecapStatus;
import com.petlog.record.repository.jpa.DiaryRepository;
import com.petlog.record.service.RecapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "Recap API", description = "AI 월간 리캡 자동 생성 및 조회 API")
@RestController
@RequestMapping("/api/recaps")
@RequiredArgsConstructor
public class RecapController {

    private final RecapService recapService;
    private final DiaryRepository diaryRepository;

    /**
     * [개선됨] 내 모든 펫에 대해 다음 달 리캡을 한 번에 예약합니다.
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

//    /**
//     * [자동 예약 기능] - 다음 달 리캡을 WAITING 상태로 예약
//     */
//    @Operation(summary = "AI 리캡 자동 예약", description = "다음 달 리캡을 WAITING 상태로 예약합니다. 스케줄러가 자동으로 생성합니다.")
//    @PostMapping("/schedule/auto")
//    public ResponseEntity<Map<String, Object>> scheduleAutoRecap(
//            @RequestParam Long petId,
//            @RequestParam Long userId,
//            @RequestParam(required = false) String petName) {
//        // 다음 달의 시작일과 종료일 계산
//        LocalDate now = LocalDate.now();
//        LocalDate nextMonthStart = now.plusMonths(1).withDayOfMonth(1);
//        LocalDate nextMonthEnd = nextMonthStart.withDayOfMonth(nextMonthStart.lengthOfMonth());
//        // WAITING 상태의 리캡 생성 요청 DTO
//        RecapRequest.Create request = RecapRequest.Create.builder()
//                .petId(petId)
//                .userId(userId)
//                .title("리캡 생성 예정")
//                .summary("다음 달 리캡이 자동으로 생성될 예정입니다.")
//                .periodStart(nextMonthStart)
//                .periodEnd(nextMonthEnd)
//                .imageUrls(List.of())
//                .momentCount(0)
//                .status("WAITING") // WAITING 상태
//                .build();
//        Long recapId = recapService.createWaitingRecap(request);
//        Map<String, Object> response = new HashMap<>();
//        response.put("recapId", recapId);
//        response.put("message", "다음 달(" + nextMonthStart.getMonthValue() + "월)의 리캡이 예약되었습니다.");
//        response.put("scheduledDate", nextMonthStart);
//        return ResponseEntity.created(URI.create("/api/recaps/" + recapId)).body(response);
//    }

    /**
     * [수정됨] 모든 펫 리캡 수동 일괄 생성
     * @RequestBody를 사용하도록 수정하여 JSON 바디 요청을 처리할 수 있게 변경했습니다.
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


//
//    /**
//     * [수동 생성 기능] - '기간 직접 선택' 버튼용
//     * 사용자가 달력에서 선택한 periodStart, periodEnd 값을 받아 리캡을 생성합니다.
//     */
//    @Operation(summary = "AI 리캡 수동 생성 (기간 지정)", description = "사용자가 직접 지정한 특정 기간의 일기를 분석하여 리캡을 생성합니다.")
//    @PostMapping("/generate/manual")
//    public ResponseEntity<Map<String, Object>> generateManualCustomRecap(@Valid @RequestBody RecapRequest.Generate request) {
//        Long recapId = recapService.createAiRecap(request);
//
//        Map<String, Object> response = new HashMap<>();
//        response.put("recapId", recapId);
//        response.put("message", "선택하신 기간(" + request.getPeriodStart() + " ~ " + request.getPeriodEnd() + ")의 추억을 분석하여 리캡을 생성했습니다.");
//
//        return ResponseEntity.created(URI.create("/api/recaps/" + recapId)).body(response);
//    }

    /**
     * [보안 강화] 리캡 상세 조회
     * @param recapId 조회할 리캡의 ID
     * @param userId 현재 로그인한 사용자의 ID (보안 검증용)
     */
    @Operation(summary = "리캡 상세 조회", description = "생성된 리캡의 상세 내용을 조회합니다. (본인 것만 조회 가능)")
    @GetMapping("/{recapId}")
    public ResponseEntity<RecapResponse.Detail> getRecap(
            @PathVariable Long recapId,
            @RequestParam Long userId)
            { // 쿼리 파라미터로 userId를 받아 서비스에 전달
        return ResponseEntity.ok(recapService.getRecap(recapId, userId));
    }

    @Operation(summary = "사용자별 리캡 목록 조회", description = "특정 사용자가 보유한 모든 리캡 목록을 조회합니다.")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<RecapResponse.Simple>> getAllRecaps(@PathVariable Long userId) {
        return ResponseEntity.ok(recapService.getAllRecaps(userId));
    }

    @Operation(summary = "펫별 리캡 목록 조회", description = "특정 펫의 리캡 역사(History)를 조회합니다.")
    @GetMapping("/pet/{petId}")
    public ResponseEntity<List<RecapResponse.Simple>> getRecapsByPet(@PathVariable Long petId) {
        return ResponseEntity.ok(recapService.getRecapsByPet(petId));
    }
}