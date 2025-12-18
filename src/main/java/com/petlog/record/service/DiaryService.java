package com.petlog.record.service;

import com.petlog.record.dto.request.DiaryRequest;
import com.petlog.record.dto.response.DiaryResponse;
import com.petlog.record.entity.Visibility;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

public interface DiaryService {

    // [수정] 날짜, locationName 파라미터 추가
    Long createAiDiary(Long userId, Long petId, MultipartFile imageFile,
                       Visibility visibility, String locationName,
                       Double latitude, Double longitude, LocalDate date);

    // 일기 단건 조회
    DiaryResponse getDiary(Long diaryId);

    // 일기 수정
    void updateDiary(Long diaryId, DiaryRequest.Update request);

    // 일기 삭제
    void deleteDiary(Long diaryId);
}