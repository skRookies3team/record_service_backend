package com.petlog.record.service;

import com.petlog.record.dto.request.DiaryRequest;
import com.petlog.record.dto.response.DiaryResponse;
import com.petlog.record.entity.Visibility;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public interface DiaryService {

    // [수정] photoArchiveId 파라미터 추가
    Long createAiDiary(Long userId, Long petId, Long photoArchiveId,
                       List<MultipartFile> imageFile, Visibility visibility,
                       String locationName, Double latitude,
                       Double longitude, LocalDate date);

    /**
     * [STEP 2] ⭐ 추가: 일기 최종 확정 및 발행
     * 사용자가 저장 버튼을 눌렀을 때 Milvus(벡터 DB) 저장 및 Kafka 이벤트를 발행합니다.
     */
    DiaryResponse confirmAndPublishDiary(Long diaryId);

    // 일기 단건 조회
    DiaryResponse getDiary(Long diaryId);

    // 일기 수정
    void updateDiary(Long diaryId, DiaryRequest.Update request);

    // 일기 삭제
    void deleteDiary(Long diaryId);
}