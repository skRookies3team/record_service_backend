package com.petlog.record.controller;

import com.petlog.record.dto.request.RecapRequest;
import com.petlog.record.dto.response.RecapResponse;
import com.petlog.record.service.RecapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "Recap API", description = "월간 리캡 생성 및 조회 API")
@RestController
@RequestMapping("/api/recaps")
@RequiredArgsConstructor
public class RecapController {

    private final RecapService recapService;

    // AI 리캡 자동 생성
    @Operation(summary = "AI 월간 리캡 생성", description = "특정 기간의 일기 데이터를 AI가 분석하여 리캡을 자동으로 생성합니다.")
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateAiRecap(@Valid @RequestBody RecapRequest.Generate request) {
        Long recapId = recapService.createAiRecap(request);

        Map<String, Object> response = new HashMap<>();
        response.put("recapId", recapId);
        response.put("message", "AI가 한 달간의 추억을 분석하여 리캡을 생성했습니다.");

        return ResponseEntity.created(URI.create("/api/recaps/" + recapId)).body(response);
    }

    // 상세 조회
    @Operation(summary = "리캡 상세 조회", description = "리캡 ID를 통해 AI 분석 내용 및 하이라이트를 조회합니다.")
    @GetMapping("/{recapId}")
    public ResponseEntity<RecapResponse.Detail> getRecap(@PathVariable Long recapId) {
        return ResponseEntity.ok(recapService.getRecap(recapId));
    }

    // 사용자별 전체 리캡 조회
    @Operation(summary = "사용자별 리캡 목록 조회", description = "특정 사용자의 모든 월간 리캡 목록을 조회합니다.")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<RecapResponse.Simple>> getAllRecaps(@PathVariable Long userId) {
        return ResponseEntity.ok(recapService.getAllRecaps(userId));
    }

    // 펫별 리캡 조회
    @Operation(summary = "펫별 리캡 목록 조회", description = "특정 펫의 월간 리캡 목록을 조회합니다.")
    @GetMapping("/pet/{petId}")
    public ResponseEntity<List<RecapResponse.Simple>> getRecapsByPet(@PathVariable Long petId) {
        return ResponseEntity.ok(recapService.getRecapsByPet(petId));
    }
}