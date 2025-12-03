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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryServiceImpl implements DiaryService {

    private final DiaryRepository diaryRepository;
    private final PhotoArchiveRepository photoArchiveRepository; // [추가] 보관함 레포지토리
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

        // 4. [핵심 로직 추가] 전체 사진 보관함(PhotoArchive)에 별도 저장
        // -> 이렇게 하면 나중에 다이어리(Diary)를 삭제해도 보관함 데이터(PhotoArchive)는 유지
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            List<PhotoArchive> archives = request.getImages().stream()
                    .map(img -> PhotoArchive.builder()
                            .userId(request.getUserId()) // 일기가 아닌 사용자에게 귀속
                            .imageUrl(img.getImageUrl())
                            .build())
                    .collect(Collectors.toList());

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
        // 요청값이 null이면 기존 값을 유지하고, 값이 있으면 업데이트
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