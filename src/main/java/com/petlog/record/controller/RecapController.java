package com.petlog.record.controller;

import com.petlog.record.dto.request.RecapRequest;
import com.petlog.record.dto.response.RecapResponse;
import com.petlog.record.service.RecapService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recaps")
@RequiredArgsConstructor
public class RecapController {

    private final RecapService recapService;

    // 리캡 임시 생성
    @PostMapping
    public ResponseEntity<Map<String, Object>> createRecap(@Valid @RequestBody RecapRequest.Create request) {
        Long recapId = recapService.createRecap(request);

        Map<String, Object> response = new HashMap<>();
        response.put("recapId", recapId);
        response.put("message", "리캡이 생성되었으며 알림이 발송되었습니다.");

        return ResponseEntity.created(URI.create("/api/recaps/" + recapId)).body(response);
    }

    // 상세 조회
    @GetMapping("/{recapId}")
    public ResponseEntity<RecapResponse.Detail> getRecap(@PathVariable Long recapId) {
        return ResponseEntity.ok(recapService.getRecap(recapId));
    }

    // 사용자별 전체 리캡 조회 (카드 리스트)
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<RecapResponse.Simple>> getAllRecaps(@PathVariable Long userId) {
        return ResponseEntity.ok(recapService.getAllRecaps(userId));
    }

    // 펫별 리캡 조회 (카드 리스트)
    @GetMapping("/pet/{petId}")
    public ResponseEntity<List<RecapResponse.Simple>> getRecapsByPet(@PathVariable Long petId) {
        return ResponseEntity.ok(recapService.getRecapsByPet(petId));
    }
}