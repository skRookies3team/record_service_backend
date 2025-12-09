package com.petlog.record.controller;

import com.petlog.record.dto.request.DiaryStyleRequest;
import com.petlog.record.dto.response.DiaryStyleResponse;
import com.petlog.record.service.DiaryStyleService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// User 클래스와 AuthenticationPrincipal은 프로젝트의 Spring Security 설정에 맞게 가상의 클래스를 사용합니다.
// 실제 프로젝트에서는 UserDetails 또는 커스텀 Principal 객체를 사용해야 합니다.
// import org.springframework.security.core.annotation.AuthenticationPrincipal; 
// import com.petlog.record.auth.User; 

@RestController
@RequestMapping("/api/v1/diary/styles")
@RequiredArgsConstructor // final 필드에 대해 생성자 주입
public class DiaryStyleController {

    private final DiaryStyleService diaryStyleService;

    // 스타일 설정 저장 (POST)
    // Upsert 로직을 포함하여, 없으면 생성하고 있으면 업데이트함
    @PostMapping
    public ResponseEntity<DiaryStyleResponse> createStyle(
        @RequestBody DiaryStyleRequest request,
        // @AuthenticationPrincipal User user // 실제 구현 시 사용
        @RequestHeader("X-USER-ID") Long userId // 테스트를 위해 임시로 userId를 직접 받거나, 헤더에서 추출한 값을 사용한다고 가정
    ) {
        // 실제 구현 시: Long userId = user.getId();
        DiaryStyleResponse response = diaryStyleService.createOrUpdateStyle(userId, request);
        return ResponseEntity.ok(response);
    }

    // 스타일 설정 수정 (PUT) - 특정 스타일 ID에 대한 전체 업데이트
    @PutMapping("/{styleId}")
    public ResponseEntity<DiaryStyleResponse> updateStyle(
        @PathVariable Long styleId,
        @RequestBody DiaryStyleRequest request,
        // @AuthenticationPrincipal User user // 실제 구현 시 사용
        // [수정 필요] @RequestHeader를 사용해 명시적으로 Header에서 userId를 받도록 합니다.
        @RequestHeader("X-USER-ID") Long userId // 테스트를 위해 임시로 userId를 직접 받거나, 헤더에서 추출한 값을 사용한다고 가정
    ) {
        // 실제 구현 시: Long userId = user.getId();
        DiaryStyleResponse response = diaryStyleService.updateStyle(styleId, request, userId);
        return ResponseEntity.ok(response);
    }

    // 사용자의 현재 스타일 설정 조회 (기본 펫, 또는 펫 ID 기준)
    // GET /api/v1/diary/styles/me?petId=1
    @GetMapping("/me")
    public ResponseEntity<DiaryStyleResponse> getMyStyle(
        // @AuthenticationPrincipal User user // 실제 구현 시 사용
        // [수정 필요] @RequestHeader를 사용해 명시적으로 Header에서 userId를 받도록 합니다.
        @RequestHeader("X-USER-ID") Long userId, // 테스트를 위해 임시로 userId를 직접 받거나, 헤더에서 추출한 값을 사용한다고 가정
        @RequestParam(required = false) Long petId
    ) {
        // 실제 구현 시: Long userId = user.getId();
        DiaryStyleResponse response = diaryStyleService.getUserStyle(userId, petId);
        return ResponseEntity.ok(response);
    }

    // 특정 펫의 스타일 설정 조회
    // GET /api/v1/diary/styles/pet/{petId}
    @GetMapping("/pet/{petId}")
    public ResponseEntity<DiaryStyleResponse> getPetStyle(
        @PathVariable Long petId,
        // @AuthenticationPrincipal User user // 실제 구현 시 사용
        // [수정 필요] @RequestHeader를 사용해 명시적으로 Header에서 userId를 받도록 합니다.
        @RequestHeader("X-USER-ID") Long userId // 테스트를 위해 임시로 userId를 직접 받거나, 헤더에서 추출한 값을 사용한다고 가정
    ) {
        // 실제 구현 시: Long userId = user.getId();
        DiaryStyleResponse response = diaryStyleService.getPetStyle(petId, userId);
        return ResponseEntity.ok(response);
    }
}