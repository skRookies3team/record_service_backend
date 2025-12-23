package com.petlog.record.controller;

import com.petlog.record.dto.response.DiaryResponse;
import com.petlog.record.service.DiaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 일기 발행 및 외부 서비스 연동 전용 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/diaries/publish") // 경로 분리
@RequiredArgsConstructor
public class DiaryPublishController {

    private final DiaryService diaryService;

    /**
     * [최종 저장] 일기 확정 및 헬스케어 서비스(Kafka) 전송
     * * 사용자가 AI 일기 내용을 확인하고 '저장' 버튼을 누르는 행위는
     * 단순히 데이터를 수정하는 것이 아니라 "발행(Publish)"이라는 액션에 가깝습니다.
     */
    @PostMapping("/{diaryId}/confirm") // 액션 중심이므로 POST 권장
    public ResponseEntity<DiaryResponse> confirmAndPublish(@PathVariable Long diaryId) {
        log.info("일기 발행 및 헬스케어 연동 시작 - diaryId: {}", diaryId);
        
        // 서비스 로직에서 Milvus 저장 + Kafka 메시지 발행을 수행합니다.
        DiaryResponse response = diaryService.confirmAndPublishDiary(diaryId);
        
        return ResponseEntity.ok(response);
    }
}