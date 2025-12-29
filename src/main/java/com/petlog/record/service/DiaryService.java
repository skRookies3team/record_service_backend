package com.petlog.record.service;

import com.petlog.record.dto.request.DiaryRequest;
import com.petlog.record.dto.response.AiDiaryResponse;
import com.petlog.record.dto.response.DiaryResponse;
import com.petlog.record.entity.Visibility;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public interface DiaryService {

//    // [수정] photoArchiveId 파라미터 추가
//    Long createAiDiary(Long userId, Long petId, Long photoArchiveId, List<DiaryRequest.Image> images,
//                       List<MultipartFile> imageFile, Visibility visibility,
//                       String locationName, Double latitude,
//                       Double longitude, LocalDate date);

    // AI 미리보기 생성 (DB 저장 X)
    AiDiaryResponse previewAiDiary(Long userId, Long petId, List<DiaryRequest.Image> images, List<MultipartFile> imageFiles);

    // 최종 일기 저장 (DB 저장 O)
    Long saveDiary(DiaryRequest.Create request);


    // 일기 단건 조회
    DiaryResponse getDiary(Long diaryId);

    // 일기 수정
    void updateDiary(Long diaryId, DiaryRequest.Update request);

    // 일기 삭제
    void deleteDiary(Long diaryId);
}