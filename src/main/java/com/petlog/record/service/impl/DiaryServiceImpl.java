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
 * [다이어리 서비스 오케스트레이터 구현체]
 * 일기 생성, 조회, 수정, 삭제의 전체 라이프사이클을 관리하며
 * AI 분석, 위치 정보 복원, 멀티 DB 연동, 비동기 이벤트 발행 등 복잡한 비즈니스 프로세스를 조율함
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

    /**
     * [AI 일기 미리보기 생성]
     * 사용자가 선택한 사진과 정보를 바탕으로 AI가 작성한 초안과 분석 데이터를 제공
     * 1. 사용자 및 펫 유효성 검증 (Feign Client)
     * 2. [위치 복원] 과거 날짜 기록 시 DB 내 수집된 위치 정보(PostGIS)를 우선적으로 복원
     * 3. 이미지 업로드 및 외부 서비스 보관함 연동
     * 4. AI 분석 수행 (LLM) 및 날씨/주소 정보 결합
     */
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

    /**
     * [다이어리 최종 저장]
     * AI 프리뷰 데이터를 바탕으로 사용자가 수정한 내용을 데이터베이스에 영구 저장
     * 1. RDB(PostgreSQL)에 일기 및 이미지 기본 정보 저장
     * 2. NoSQL(MongoDB)에 이미지별 비정형 메타데이터 저장
     * 3. [위치 저장] 과거 날짜에 위치 좌표가 포함된 경우 이동 경로 데이터로 역추적 저장
     * 4. 트랜잭션 성공 시 비동기 이벤트 발행 (Kafka, VectorDB)
     */
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

    /**
     * [다이어리 상세 조회]
     * RDB의 기본 정보와 MongoDB의 비정형 메타데이터, 그리고 스타일 설정을 결합하여 반환
     */
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

    /**
     * [다이어리 정보 수정]
     * 기존에 저장된 일기의 제목, 본문, 날짜 및 공개 범위 등을 변경
     * 엔티티의 update 메서드를 호출하여 Dirty Checking(변경 감지) 기능을 통해 DB에 반영
     * 수정 완료 후, 외부 서비스 동기화를 위해 DiaryUpdatedEvent를 발행
     */
    @Override
    @Transactional
    public void updateDiary(Long diaryId, DiaryRequest.Update request) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.DIARY_NOT_FOUND));

        diary.update(request.getTitle(), request.getContent(), request.getDate(),
                request.getVisibility(), request.getWeather(), request.getMood());

        eventPublisher.publishEvent(new DiaryUpdatedEvent(diary));
    }

    /**
     * [다이어리 삭제]
     * 일기 데이터 삭제 및 연관된 MongoDB 메타데이터 정리, Kafka 삭제 이벤트 전송
     */
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

    /**
     * [비동기 이벤트 핸들러: 일기 생성 시]
     * 메인 트랜잭션 커밋 후 별도 스레드에서 Vector DB 저장 및 Kafka 메시지 발행 수행
     * 사용자 응답 속도를 저해하지 않기 위해 비동기로 처리
     */
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

    /**
     * [비동기 이벤트 핸들러: 일기 수정 시]
     * 메인 트랜잭션이 성공적으로 커밋된(AFTER_COMMIT) 직후 별도 스레드에서 실행
     * 시맨틱 검색 엔진(Vector DB)과 마이크로서비스 간 데이터 동기화(Kafka)를 담당
     */
    @Async
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDiaryUpdated(DiaryUpdatedEvent event) {
        Diary diary = event.diary();
        diaryVectorService.saveToVectorDB(diary);
        diaryEventProducer.publishDiaryUpdatedEvent(diary.getDiaryId(), diary.getUserId(),
                diary.getPetId(), diary.getContent());
    }


    /** [이벤트 객체] 다이어리 생성/수정 정보를 담는 Immutable Record */
    public record DiaryCreatedEvent(Diary diary) {}
    public record DiaryUpdatedEvent(Diary diary) {}

    /**
     * [사용자 및 반려동물 유효성 검증]
     * Feign Client를 사용하여 유저 및 펫 마이크로서비스로부터 실제 데이터 존재 여부를 확인
     * 서비스 간의 논리적 무결성을 보장하기 위한 사전 체크 단계
     */
    private void validateUserAndPet(Long userId, Long petId) {
        try { userClient.getUserInfo(userId); } catch (Exception e) { throw new EntityNotFoundException(ErrorCode.USER_NOT_FOUND); }
        try { petClient.getPetInfo(petId); } catch (FeignException e) { throw new EntityNotFoundException(ErrorCode.PET_NOT_FOUND); }
    }

    /**
     * [멀티파트 파일 유효성 검사]
     * 업로드된 파일 리스트가 실제로 데이터를 포함하고 있는지 확인 (null 및 Empty 체크)
     */
    private boolean isActualFilePresent(List<MultipartFile> files) {
        return files != null && !files.isEmpty() && !files.get(0).isEmpty();
    }
}