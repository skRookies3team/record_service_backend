package com.example.petlog.service.impl;

import com.example.petlog.client.PetServiceClient;
import com.example.petlog.client.StorageServiceClient;
import com.example.petlog.client.UserServiceClient;
import com.example.petlog.entity.DiaryImage;
import com.example.petlog.entity.ImageSource;
import com.example.petlog.dto.request.DiaryRequest;
import com.example.petlog.dto.response.DiaryResponse;
import com.example.petlog.entity.Diary;
import com.example.petlog.exception.EntityNotFoundException;
import com.example.petlog.exception.ErrorCode;
import com.example.petlog.repository.DiaryRepository;
import com.example.petlog.service.DiaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryServiceImpl implements DiaryService {

    private final DiaryRepository diaryRepository;

    // [수정] MockStorageServiceClient 이름을 가진 빈을 주입하도록 명시
    // Mock이 로드되지 않는 환경에서는 Feign Client가 주입됩니다.
    @Qualifier("mockStorageServiceClient")
    private final StorageServiceClient storageServiceClient;
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

        // 2. DTO -> Entity 변환
        Diary diary = request.toEntity();

        // 3. 일기 저장
        Diary savedDiary = diaryRepository.save(diary);

        // 4. 사진 보관함 처리 로직
        List<DiaryImage> images = diary.getImages();

        if (!images.isEmpty()) {
            // [핵심] 4-1. 외부 보관함 서비스로 전송할 사진 선별 (GALLERY 출처만)
            List<StorageServiceClient.PhotoRequest> newPhotos = images.stream()
                    .filter(img -> img.getSource() == ImageSource.GALLERY) // 갤러리에서 온 것만 필터링
                    .map(img -> new StorageServiceClient.PhotoRequest(
                            img.getUserId(),
                            img.getImageUrl()
                    ))
                    .collect(Collectors.toList());

            // 3-2. 선별된 사진만 외부 서비스로 전송
            if (!newPhotos.isEmpty()) {
                try {
                    storageServiceClient.savePhotos(newPhotos);
                    log.info("외부 보관함 서비스에 새 사진 {}장 전송 완료. URL: [{}]", newPhotos.size(), newPhotos.stream().map(StorageServiceClient.PhotoRequest::imageUrl).collect(Collectors.joining(", ")));
                } catch (Exception e) {
                    log.warn("외부 보관함 서비스 전송 실패: {}", e.getMessage());
                }
            }
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

        // DiaryImage는 Cascade에 의해 함께 삭제됩니다.
        diaryRepository.delete(diary);
    }
}