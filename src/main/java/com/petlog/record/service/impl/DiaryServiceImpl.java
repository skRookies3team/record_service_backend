package com.petlog.record.service.impl;

import com.petlog.record.client.PetClient;
import com.petlog.record.client.UserClient;
import com.petlog.record.dto.client.ArchiveResponse;
import com.petlog.record.dto.request.DiaryRequest;
import com.petlog.record.dto.response.AiDiaryResponse;
import com.petlog.record.dto.response.DiaryResponse;
import com.petlog.record.dto.response.LocationResponse;
import com.petlog.record.entity.*;
import com.petlog.record.exception.BusinessException;
import com.petlog.record.exception.EntityNotFoundException;
import com.petlog.record.exception.ErrorCode;
import com.petlog.record.infrastructure.kafka.DiaryEventProducer;
import com.petlog.record.repository.jpa.DiaryArchiveRepository;
import com.petlog.record.repository.jpa.DiaryRepository;
import com.petlog.record.service.*;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 일기 서비스 구현체 (Orchestrator)
 * 이제 구체 클래스가 아닌 인터페이스(DiaryAiService 등)에 의존합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryServiceImpl implements DiaryService {

    private final DiaryRepository diaryRepository;
    private final DiaryStyleService diaryStyleService;
    private final DiaryArchiveRepository diaryArchiveRepository;
    private final UserClient userClient;
    private final PetClient petClient;
    private final DiaryEventProducer diaryEventProducer;
    private final ApplicationEventPublisher eventPublisher;

    // 인터페이스 타입으로 주입 (DIP 원칙 준수)
    private final DiaryAiService diaryAiService;
    private final DiaryVectorService diaryVectorService;
    private final ExternalApiService externalApiService;
    private final DiaryMediaService diaryMediaService;
    private final LocationService locationService; // ✅ 위치 조회 서비스 주입 확인

    @Override
    @Transactional
    public AiDiaryResponse previewAiDiary(Long userId, Long petId, List<DiaryRequest.Image> images, List<MultipartFile> imageFiles, Double latitude, Double longitude, String date) {
        log.info("AI Diary Preview started. User: {}, Pet: {}", userId, petId);
        validateUserAndPet(userId, petId);

        LocalDate diaryDate = (date != null && !date.isEmpty()) ? LocalDate.parse(date) : LocalDate.now();

        // 1. 위치 복원 로직: 과거 날짜인 경우 DB 기록을 최우선으로 조회
        Double targetLat = latitude;
        Double targetLng = longitude;
        boolean useDbLocation = false; // DB 위치 사용 여부 플래그

        if (diaryDate.isBefore(LocalDate.now())) {
            LocationResponse savedLocation = locationService.getRepresentativeLocation(userId, diaryDate);
            if (savedLocation != null) {
                // ✅ DB 위치 있음 → DB 좌표 사용
                targetLat = savedLocation.getLatitude();
                targetLng = savedLocation.getLongitude();
                useDbLocation = true;
                log.info("과거 날짜({}) DB 위치 복원 성공: ({}, {})", diaryDate, targetLat, targetLng);
            } else {
                // ✅ DB 위치 없음 → 기본 위치 좌표만 사용 (이름은 AI 값 유지)
                log.warn("과거 날짜({}) DB 위치 없음 → 기본 위치로 폴백", diaryDate);
                try {
                    LocationResponse defaultLocation = locationService.getRepresentativeLocation(userId, LocalDate.now());
                    if (defaultLocation != null) {
                        targetLat = defaultLocation.getLatitude();
                        targetLng = defaultLocation.getLongitude();
                        log.info("기본 위치 사용: ({}, {})", targetLat, targetLng);
                    }
                } catch (Exception e) {
                    log.error("기본 위치 조회 실패", e);
                }
            }
        } else if (targetLat == null || targetLng == null) {
            // 오늘 날짜인데 좌표가 없는 경우에만 보조적으로 조회
            LocationResponse savedLocation = locationService.getRepresentativeLocation(userId, diaryDate);
            if (savedLocation != null) {
                targetLat = savedLocation.getLatitude();
                targetLng = savedLocation.getLongitude();
            }
        }

        // 2. 이미지 처리
        List<String> finalImageUrls = new ArrayList<>();
        List<Long> finalArchiveIds = new ArrayList<>();

        if (images != null) {
            for (DiaryRequest.Image img : images) {
                if (ImageSource.ARCHIVE.equals(img.getSource()) && img.getArchiveId() != null) {
                    finalImageUrls.add(img.getImageUrl());
                    finalArchiveIds.add(img.getArchiveId());
                }
            }
        }

        if (isActualFilePresent(imageFiles)) {
            ArchiveResponse.CreateArchiveDtoList archiveResponse = diaryMediaService.uploadToArchive(userId, imageFiles);
            for (ArchiveResponse.CreateArchiveDto dto : archiveResponse.getArchives()) {
                finalImageUrls.add(dto.getUrl());
                finalArchiveIds.add(dto.getArchiveId());
            }
        }

        if (finalImageUrls.isEmpty()) throw new BusinessException(ErrorCode.INVALID_PARAMETER);

        // 3. AI 분석 수행
        AiDiaryResponse aiResponse = diaryAiService.generateContentWithAiFromUrls(finalImageUrls);

        // 4. 응답 데이터 설정
        aiResponse.setDate(diaryDate);
        aiResponse.setImageUrls(finalImageUrls);
        aiResponse.setArchiveIds(finalArchiveIds);

        // 5. 좌표 설정
        aiResponse.setLatitude(targetLat);
        aiResponse.setLongitude(targetLng);

        // 6. 위치 이름 설정 (DB 위치 있을 때만)
        if (useDbLocation && targetLat != null && targetLng != null) {
            String resolvedLocationName = externalApiService.getAddressFromCoords(targetLat, targetLng);
            if (resolvedLocationName != null && !resolvedLocationName.isEmpty()) {
                aiResponse.setLocationName(resolvedLocationName);
                log.info("DB 위치명 설정: {}", resolvedLocationName);
            }
        }
        // DB 위치 없으면 AI가 분석한 locationName 그대로 유지

        // 7. 날씨 정보
        String weatherInfo = externalApiService.getWeatherInfo(diaryDate, targetLat, targetLng);
        if (weatherInfo != null) {
            aiResponse.setWeather(weatherInfo);
        }

        return aiResponse;
    }

    @Override
    @Transactional
    public Long saveDiary(DiaryRequest.Create request) {
        LocalDate diaryDate = request.getDate() != null ? request.getDate() : LocalDate.now();

        // 1. 위치 정보 복원 (과거 날짜인 경우 DB 우선)
        Double targetLat = request.getLatitude();
        Double targetLng = request.getLongitude();

        if (diaryDate.isBefore(LocalDate.now())) {
            LocationResponse savedLocation = locationService.getRepresentativeLocation(request.getUserId(), diaryDate);
            if (savedLocation != null) {
                targetLat = savedLocation.getLatitude();
                targetLng = savedLocation.getLongitude();
                log.info("과거 날짜({}) 저장 시 DB 위치 사용", diaryDate);
            }
        }

        // 2. 날씨 정보 조회
        String weatherInfo = request.getWeather();
        if (weatherInfo == null || weatherInfo.isEmpty()) {
            weatherInfo = externalApiService.getWeatherInfo(diaryDate, targetLat, targetLng);
            if (weatherInfo == null) weatherInfo = "맑은 날씨";
        }

        // 3. 위치명 조회 (복원된 좌표 사용)
        String finalLocationName = request.getLocationName();
        if ((finalLocationName == null || finalLocationName.isEmpty()) && targetLat != null && targetLng != null) {
            finalLocationName = externalApiService.getAddressFromCoords(targetLat, targetLng);
        }

        // 4. Diary 엔티티 생성
        Diary diary = Diary.builder()
                .userId(request.getUserId())
                .petId(request.getPetId())
                .title(request.getTitle())
                .content(request.getContent())
                .mood(request.getMood())
                .weather(weatherInfo)
                .isAiGen(request.getIsAiGen())
                .visibility(request.getVisibility() != null ? request.getVisibility() : Visibility.PRIVATE)
                .latitude(targetLat)
                .longitude(targetLng)
                .locationName(finalLocationName)
                .date(diaryDate)
                .build();

        // 5. 이미지 추가
        if (request.getImages() != null) {
            for (DiaryRequest.Image imageDto : request.getImages()) {
                diary.addImage(DiaryImage.builder()
                        .imageUrl(imageDto.getImageUrl())
                        .userId(request.getUserId())
                        .imgOrder(imageDto.getImgOrder())
                        .mainImage(imageDto.getMainImage())
                        .source(imageDto.getSource())
                        .build());
            }
        }

        // 6. Diary 저장
        Diary savedDiary = diaryRepository.save(diary);

        // 7. ✅ [NEW] 과거 날짜 + 위치 있음 → DB에 위치 저장
        if (diaryDate.isBefore(LocalDate.now()) && targetLat != null && targetLng != null) {
            try {
                locationService.saveLocation(request.getUserId(), diaryDate, targetLat, targetLng, finalLocationName);
                log.info("과거 날짜({}) 위치 저장 성공: ({}, {}) - {}", diaryDate, targetLat, targetLng, finalLocationName);
            } catch (Exception e) {
                log.error("과거 날짜 위치 저장 실패", e);
                // 저장 실패해도 일기는 저장됨
            }
        }

        // 8. 이미지 메타데이터 저장
        if (request.getImages() != null) {
            Map<String, Long> urlToIdMap = savedDiary.getImages().stream()
                    .collect(Collectors.toMap(DiaryImage::getImageUrl, DiaryImage::getImageId));

            for (DiaryRequest.Image imageDto : request.getImages()) {
                if (imageDto.getMetadata() != null && !imageDto.getMetadata().isEmpty()) {
                    Long savedImageId = urlToIdMap.get(imageDto.getImageUrl());
                    if (savedImageId != null) {
                        diaryMediaService.savePhotoMetadata(savedImageId, imageDto.getMetadata());
                    }
                }
            }
        }

        // 9. Archive 연결
        if (request.getArchiveIds() != null) {
            for (Long archiveId : request.getArchiveIds()) {
                diaryArchiveRepository.save(DiaryArchive.create(savedDiary, archiveId));
            }
        }

        // 10. 이벤트 발행
        eventPublisher.publishEvent(new DiaryCreatedEvent(savedDiary));

        return savedDiary.getDiaryId();
    }

    @Override
    public DiaryResponse getDiary(Long diaryId) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.DIARY_NOT_FOUND));

        List<Long> imageIds = diary.getImages().stream().map(DiaryImage::getImageId).toList();
        Map<Long, Map<String, Object>> metadataMap = diaryMediaService.getMetadataMap(imageIds);

        List<DiaryResponse.Image> imageDtos = diary.getImages().stream()
                .map(img -> DiaryResponse.Image.fromEntity(img, metadataMap.get(img.getImageId())))
                .toList();

        DiaryResponse response = DiaryResponse.fromEntity(diary);
        response.setImages(imageDtos);

        try {
            response.setStyle(diaryStyleService.getDiaryStyle(diaryId));
        } catch (Exception e) {
            log.warn("Style not found for diary {}", diaryId);
        }
        return response;
    }

    @Override
    @Transactional
    public void updateDiary(Long diaryId, DiaryRequest.Update request) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.DIARY_NOT_FOUND));

        diary.update(request.getTitle(), request.getContent(), request.getDate(),
                request.getVisibility(), request.getWeather(), request.getMood());

        eventPublisher.publishEvent(new DiaryUpdatedEvent(diary));
    }

    @Override
    @Transactional
    public void deleteDiary(Long diaryId) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.DIARY_NOT_FOUND));

        Long userId = diary.getUserId();
        Long petId = diary.getPetId();
        List<Long> imageIds = diary.getImages().stream().map(DiaryImage::getImageId).toList();

        diaryMediaService.deleteMetadataByImageIds(imageIds);
        diaryRepository.delete(diary);

        try {
            diaryEventProducer.publishDiaryDeletedEvent(diaryId, userId, petId);
        } catch (Exception e) {
            log.error("Kafka 삭제 이벤트 발행 실패: {}", e.getMessage());
        }
    }

    @Async
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDiaryCreated(DiaryCreatedEvent event) {
        Diary diary = event.diary();
        diaryVectorService.saveToVectorDB(diary);

        String firstUrl = diary.getImages().isEmpty() ? null : diary.getImages().get(0).getImageUrl();
        diaryEventProducer.publishDiaryCreatedEvent(diary.getDiaryId(), diary.getUserId(),
                diary.getPetId(), diary.getContent(), firstUrl);
    }

    @Async
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDiaryUpdated(DiaryUpdatedEvent event) {
        Diary diary = event.diary();
        diaryVectorService.saveToVectorDB(diary);
        diaryEventProducer.publishDiaryUpdatedEvent(diary.getDiaryId(), diary.getUserId(),
                diary.getPetId(), diary.getContent());
    }

    public record DiaryCreatedEvent(Diary diary) {}
    public record DiaryUpdatedEvent(Diary diary) {}

    private void validateUserAndPet(Long userId, Long petId) {
        try { userClient.getUserInfo(userId); } catch (Exception e) { throw new EntityNotFoundException(ErrorCode.USER_NOT_FOUND); }
        try { petClient.getPetInfo(petId); } catch (FeignException e) { throw new EntityNotFoundException(ErrorCode.PET_NOT_FOUND); }
    }

    private boolean isActualFilePresent(List<MultipartFile> files) {
        return files != null && !files.isEmpty() && !files.get(0).isEmpty();
    }
}