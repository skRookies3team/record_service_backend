package com.petlog.record.service.impl;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecapServiceImpl implements RecapService {

    private final RecapRepository recapRepository;
    private final DiaryRepository diaryRepository;
    private final RecapAiService recapAiService;

    @Override
    @Transactional
    public Long createAiRecap(RecapRequest.Generate request) {
        log.info("[Recap] AI 리캡 생성 프로세스 시작 - 펫 ID: {}, 기간: {} ~ {}",
                request.getPetId(), request.getPeriodStart(), request.getPeriodEnd());

        // 1. 해당 기간의 일기 목록 조회
        List<Diary> diaries = diaryRepository.findAllByPetIdAndDateBetween(
                request.getPetId(),
                request.getPeriodStart(),
                request.getPeriodEnd()
        );

        if (diaries.isEmpty()) {
            log.warn("[Recap] 해당 기간에 작성된 일기가 없어 생성을 중단합니다. (Pet ID: {})", request.getPetId());
            throw new RuntimeException("해당 기간에 작성된 일기가 없어 리캡을 생성할 수 없습니다.");
        }

        // 2. 일기별 대표 이미지(mainImage == true) 추출 및 랜덤 8장 선정
        List<String> representativeImages = diaries.stream()
                .flatMap(diary -> diary.getImages().stream()) // List<DiaryImage>를 스트림으로 평탄화
                .filter(img -> Boolean.TRUE.equals(img.getMainImage())) // 대표 이미지만 필터링
                .map(DiaryImage::getImageUrl) // 필드명 반영: getImageUrl
                .filter(Objects::nonNull)
                .filter(url -> !url.isBlank())
                .collect(Collectors.toList());

        log.info("[Recap] 추출된 총 대표 이미지 개수: {}개", representativeImages.size());

        // 리스트를 무작위로 섞은 후 최대 8장까지만 선택
        Collections.shuffle(representativeImages);
        List<String> selectedImages = representativeImages.stream()
                .limit(8)
                .collect(Collectors.toList());

        log.info("[Recap] 리캡에 포함될 최종 이미지 개수: {}개", selectedImages.size());

        // 3. AI 분석을 위한 텍스트 리스트 추출
        List<String> diaryTexts = diaries.stream()
                .map(Diary::getContent)
                .collect(Collectors.toList());

        // 4. AI 서비스 호출에 필요한 정보 준비
        int year = request.getPeriodStart().getYear();
        int month = request.getPeriodStart().getMonthValue();
        String petName = (request.getPetName() != null && !request.getPetName().isBlank())
                ? request.getPetName() : "우리 아이";

        // 5. AI 서비스 호출
        RecapAiResponse aiData = recapAiService.analyzeMonth(petName, year, month, diaryTexts);

        // 6. Recap 엔티티 생성 및 이미지 리스트 저장
        Recap recap = Recap.builder()
                .userId(request.getUserId())
                .petId(request.getPetId())
                .title(aiData.getTitle())
                .summary(aiData.getSummary())
                .periodStart(request.getPeriodStart())
                .periodEnd(request.getPeriodEnd())
                .imageUrls(selectedImages) // 추출된 랜덤 이미지 리스트 저장
                .momentCount(diaries.size())
                .status(RecapStatus.GENERATED)
                .build();

        // 하이라이트 추가
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

        return savedRecap.getRecapId();
    }

    @Override
    @Transactional
    public Long createWaitingRecap(RecapRequest.Create request) {
        log.info("[Recap] WAITING 상태 리캡 생성 - 펫 ID: {}, 기간: {} ~ {}",
                request.getPetId(), request.getPeriodStart(), request.getPeriodEnd());

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

    @Override
    public RecapResponse.Detail getRecap(Long recapId) {
        Recap recap = recapRepository.findById(recapId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.RECAP_NOT_FOUND));

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
}