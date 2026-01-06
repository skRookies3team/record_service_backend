package com.petlog.record.service.impl;

import com.petlog.record.client.NotificationClient;
import com.petlog.record.client.UserClient;
import com.petlog.record.dto.client.NotificationRequest;
import com.petlog.record.dto.request.RecapRequest;
import com.petlog.record.dto.response.RecapAiResponse;
import com.petlog.record.dto.response.RecapResponse;
import com.petlog.record.entity.Diary;
import com.petlog.record.entity.DiaryImage;
import com.petlog.record.entity.Recap;
import com.petlog.record.entity.RecapHighlight;
import com.petlog.record.entity.RecapStatus;
import com.petlog.record.exception.EntityNotFoundException;
import com.petlog.record.exception.ErrorCode;
import com.petlog.record.repository.jpa.DiaryRepository;
import com.petlog.record.repository.jpa.RecapRepository;
import com.petlog.record.service.RecapAiService;
import com.petlog.record.service.RecapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecapServiceImpl implements RecapService {

    private final RecapRepository recapRepository;
    private final DiaryRepository diaryRepository;
    private final RecapAiService recapAiService;
    private final NotificationClient notificationClient; // 리캡 알림 연동
    private final UserClient userClient;  // 리캡 코인 연동

    @Override
    @Transactional
    public Long createAiRecap(RecapRequest.Generate request) {
        log.info("[Recap] AI 리캡 생성 프로세스 시작 - User: {}, Pet: {}", request.getUserId(), request.getPetId());

        // 1. 해당 펫의 일기 조회 (위에서 검증했으므로 안전하게 해당 펫의 데이터만 가져옴)
        List<Diary> diaries = diaryRepository.findAllByPetIdAndDateBetween(
                request.getPetId(),
                request.getPeriodStart(),
                request.getPeriodEnd()
        );

        if (diaries.isEmpty()) {
            log.warn("[Recap] 해당 기간에 작성된 일기가 없어 생성을 중단합니다. (Pet ID: {})", request.getPetId());
            throw new RuntimeException("해당 기간에 작성된 일기가 없어 리캡을 생성할 수 없습니다.");
        }

        // 2. [보안 강화] 조회된 일기들이 요청한 사용자의 것인지 검증 (Diary 데이터를 활용한 소유권 확인)
        verifyDiaryOwnership(diaries, request.getUserId());

        // 일기별 대표 이미지(mainImage == true) 추출 및 랜덤 8장 선정
        List<String> representativeImages = diaries.stream()
                .flatMap(diary -> diary.getImages().stream())
                .filter(img -> Boolean.TRUE.equals(img.getMainImage()))
                .map(DiaryImage::getImageUrl)
                .filter(Objects::nonNull)
                .filter(url -> !url.isBlank())
                .collect(Collectors.toList());

        Collections.shuffle(representativeImages);
        List<String> selectedImages = representativeImages.stream()
                .limit(8)
                .collect(Collectors.toList());

        // AI 분석용 텍스트 추출
        List<String> diaryTexts = diaries.stream()
                .map(Diary::getContent)
                .collect(Collectors.toList());

        int year = request.getPeriodStart().getYear();
        int month = request.getPeriodStart().getMonthValue();
        String petName = (request.getPetName() != null && !request.getPetName().isBlank())
                ? request.getPetName() : "우리 아이";

        // AI 분석 수행
        RecapAiResponse aiData = recapAiService.analyzeMonth(petName, year, month, diaryTexts);

        // 4. 리캡 저장 (userId 포함)
        Recap recap = Recap.builder()
                .userId(request.getUserId())
                .petId(request.getPetId())
                .title(aiData.getTitle())
                .summary(aiData.getSummary())
                .periodStart(request.getPeriodStart())
                .periodEnd(request.getPeriodEnd())
                .imageUrls(selectedImages)
                .momentCount(diaries.size())
                .status(RecapStatus.GENERATED)
                .build();

        if (aiData.getHighlights() != null) {
            aiData.getHighlights().forEach(h ->
                    recap.addHighlight(RecapHighlight.builder()
                            .title(h.getTitle())
                            .content(h.getContent())
                            .build())
            );
        }

        Recap savedRecap = recapRepository.save(recap);
        log.info("[Recap] AI 리캡 저장 완료 - Recap ID: {}", savedRecap.getRecapId());

        // ✅ [NEW] 코인 적립 (30 코인)
        try {
            Map<String, Object> coinRequest = new HashMap<>();
            coinRequest.put("amount", 30L);
            coinRequest.put("type", "RECAP");

            userClient.earnCoin(request.getUserId(), coinRequest);
            log.info("[Coin] 리캡 생성 코인 적립 완료: userId={}, amount=30", request.getUserId());
        } catch (Exception e) {
            log.error("[Coin] 코인 적립 실패 (리캡은 정상 생성됨): {}", e.getMessage());
        }

        // ✅ [추가] 리캡 생성 알림 전송
        try {
            NotificationRequest notificationRequest = NotificationRequest.builder()
                    .type("RECAP")
                    .senderId(request.getUserId())
                    .receiverId(request.getUserId())
                    .targetId(savedRecap.getRecapId())
                    .coin(30L)
                    .build();

            notificationClient.createNotification(notificationRequest);
            log.info("[Notification] 리캡 생성 알림 전송 완료: userId={}, recapId={}",
                    request.getUserId(), savedRecap.getRecapId());
        } catch (Exception e) {
            log.error("[Notification] 알림 전송 실패 (리캡은 정상 생성됨): {}", e.getMessage());
        }

        return savedRecap.getRecapId();
    }

    /**
     * WAITING 상태의 리캡을 단순히 생성만 합니다.
     *
     */
    @Override
    @Transactional
    public Long createWaitingRecap(RecapRequest.Create request) {
        log.info("[Recap] WAITING 리캡 예약 시작 - User: {}, Pet: {}", request.getUserId(), request.getPetId());

        // 예약 시점에는 분석할 일기가 없을 수 있으므로,
        // 다이어리 테이블에서 해당 유저가 해당 펫의 일기를 한 번이라도 작성했는지 여부로 최소한의 검증을 수행할 수 있습니다.
        boolean hasHistory = diaryRepository.existsByPetIdAndUserId(request.getPetId(), request.getUserId());
        if (!hasHistory) {
            throw new AccessDeniedException("해당 반려동물에 대한 기록 권한이 없거나 작성된 일기가 없습니다.");
        }

        // WAITING 상태로 리캡 생성 (AI 분석 없이)
        Recap recap = Recap.builder()
                .userId(request.getUserId())
                .petId(request.getPetId())
                .title(request.getTitle())
                .summary(request.getSummary())
                .periodStart(request.getPeriodStart())
                .periodEnd(request.getPeriodEnd())
                .imageUrls(List.of()) // 빈 리스트
                .momentCount(0)
                .status(RecapStatus.WAITING) // WAITING 상태
                .build();

        Recap savedRecap = recapRepository.save(recap);
        log.info("[Recap] WAITING 리캡 저장 완료 - Recap ID: {}", savedRecap.getRecapId());

        return savedRecap.getRecapId();
    }

    /**
     * 리캡 상세 조회 (보안 강화 버전)
     * @param recapId 조회할 리캡 ID
     * @param userId 현재 로그인한 사용자의 ID (검증용)
     */
    @Override
    @Transactional(readOnly = true)
    public RecapResponse.Detail getRecap(Long recapId, Long userId) {
        Recap recap = recapRepository.findById(recapId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.RECAP_NOT_FOUND));

        // [보안 로직] 리캡의 소유자가 현재 요청한 유저와 일치하는지 검증
        if (!recap.getUserId().equals(userId)) {
            log.warn("[Security] 권한 없는 리캡 접근 시도 - User: {}, RecapID: {}", userId, recapId);
            throw new AccessDeniedException("해당 리캡을 조회할 권한이 없습니다.");
        }
        // Lazy loading 초기화
        recap.getImageUrls().size();
        recap.getHighlights().size();
        return RecapResponse.Detail.fromEntity(recap);
    }


    @Override
    public List<RecapResponse.Simple> getAllRecaps(Long userId) {
        return recapRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(RecapResponse.Simple::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<RecapResponse.Simple> getRecapsByPet(Long petId) {
        return recapRepository.findAllByPetIdOrderByCreatedAtDesc(petId).stream()
                .map(RecapResponse.Simple::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * [Helper] 일기 데이터를 활용한 소유권 검증 로직
     * 조회된 일기 목록 중 요청한 사용자의 ID와 일치하지 않는 데이터가 있는지 확인합니다.
     */
    private void verifyDiaryOwnership(List<Diary> diaries, Long userId) {
        boolean isNotOwner = diaries.stream()
                .anyMatch(diary -> !diary.getUserId().equals(userId));

        if (isNotOwner) {
            log.error("[Security] 일기 소유권 불일치 감지 - 요청 유저: {}", userId);
            throw new AccessDeniedException("본인의 반려동물 기록만 리캡으로 생성할 수 있습니다.");
        }
    }
}