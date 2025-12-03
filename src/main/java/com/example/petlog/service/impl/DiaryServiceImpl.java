package com.example.petlog.service.impl;

import com.example.petlog.client.PetServiceClient;
import com.example.petlog.client.UserServiceClient;
import com.example.petlog.dto.request.DiaryRequest;
import com.example.petlog.dto.response.DiaryResponse;
import com.example.petlog.entity.Diary;
import com.example.petlog.entity.PhotoArchive;
import com.example.petlog.exception.EntityNotFoundException;
import com.example.petlog.exception.ErrorCode;
import com.example.petlog.repository.DiaryRepository;
import com.example.petlog.repository.PhotoArchiveRepository;
import com.example.petlog.service.DiaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryServiceImpl implements DiaryService {

    private final DiaryRepository diaryRepository;
    private final PhotoArchiveRepository photoArchiveRepository;
    private final UserServiceClient userClient;
    private final PetServiceClient petClient;

    @Override
    @Transactional
    public Long createDiary(DiaryRequest.Create request) {
        // 1. MSA 검증 (로컬 테스트 시 주석 처리)
        /*
        if (!userClient.checkUserExists(request.getUserId())) {
            throw new EntityNotFoundException(ErrorCode.USER_NOT_FOUND);
        }
        if (!petClient.checkPetExists(request.getPetId())) {
            throw new EntityNotFoundException(ErrorCode.PET_NOT_FOUND);
        }
        */

        // 2. DTO -> Entity 변환 (일기 및 일기용 이미지 생성)
        Diary diary = request.toEntity();

        // 3. 일기 저장 (Cascade 설정으로 인해 DiaryImage 테이블에도 자동 저장됨)
        Diary savedDiary = diaryRepository.save(diary);

        // 4. [핵심 로직] 전체 사진 보관함(PhotoArchive) 별도 저장
        // -> DTO 내부 메서드를 사용하여 변환 로직을 위임 (서비스 코드 간소화)
        List<PhotoArchive> archives = request.toPhotoArchiveEntities();
        if (!archives.isEmpty()) {
            photoArchiveRepository.saveAll(archives);
        }

        return savedDiary.getDiaryId();
    }

    @Override
    public DiaryResponse getDiary(Long diaryId) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.DIARY_NOT_FOUND));

        return DiaryResponse.fromEntity(diary);
    }

    @Override
    @Transactional
    public void updateDiary(Long diaryId, DiaryRequest.Update request) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.DIARY_NOT_FOUND));

        // PATCH (부분 수정) 로직
        diary.update(
                request.getContent() != null ? request.getContent() : diary.getContent(),
                request.getVisibility() != null ? request.getVisibility() : diary.getVisibility(),
                request.getWeather() != null ? request.getWeather() : diary.getWeather(),
                request.getMood() != null ? request.getMood() : diary.getMood()
        );
    }

    @Override
    @Transactional
    public void deleteDiary(Long diaryId) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.DIARY_NOT_FOUND));

        // 1. 다이어리 삭제
        // -> 연관된 DiaryImage(일기 속 사진)들은 CascadeType.ALL에 의해 함께 삭제
        // -> 하지만 위(createDiary)에서 따로 저장한 PhotoArchive(보관함 사진)는 삭제되지 않고 안전하게 남음
        diaryRepository.delete(diary);
    }
}