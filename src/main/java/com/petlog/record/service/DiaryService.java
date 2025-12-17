package com.petlog.record.service;

import com.petlog.record.dto.request.DiaryRequest;
import com.petlog.record.dto.response.DiaryResponse;
import com.petlog.record.entity.Visibility;
import org.springframework.web.multipart.MultipartFile;

public interface DiaryService {

    // [수정] AI 일기 생성 (위도, 경도 추가)
    Long createAiDiary(Long userId, Long petId, MultipartFile imageFile, Visibility visibility, Double latitude, Double longitude);


    // 일기 단건 조회
    DiaryResponse getDiary(Long diaryId);

    // 일기 수정
    void updateDiary(Long diaryId, DiaryRequest.Update request);

    // 일기 삭제
    void deleteDiary(Long diaryId);
}