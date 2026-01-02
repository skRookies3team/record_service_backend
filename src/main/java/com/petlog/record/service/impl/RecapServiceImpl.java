package com.petlog.record.service.impl;

import com.petlog.record.dto.request.RecapRequest;
import com.petlog.record.dto.response.RecapAiResponse;
import com.petlog.record.dto.response.RecapResponse;
import com.petlog.record.entity.Diary;
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

import java.util.List;
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
        log.info("AI 리캡 생성 시작 - 펫 ID: {}", request.getPetId());

        // 1. 해당 기간의 일기 목록 조회
        List<Diary> diaries = diaryRepository.findAllByPetIdAndDateBetween(
                request.getPetId(),
                request.getPeriodStart(),
                request.getPeriodEnd()
        );

        if (diaries.isEmpty()) {
            throw new RuntimeException("해당 기간에 작성된 일기가 없어 리캡을 생성할 수 없습니다.");
        }

        // 2. AI 분석을 위한 텍스트 리스트 추출
        List<String> diaryTexts = diaries.stream()
                .map(Diary::getContent)
                .collect(Collectors.toList());

        // 3. 연도와 월 추출
        int year = request.getPeriodStart().getYear();
        int month = request.getPeriodStart().getMonthValue();
        String petName = (request.getPetName() != null) ? request.getPetName() : "우리 아이";

        // 4. AI 서비스 호출
        RecapAiResponse aiData = recapAiService.analyzeMonth(petName, year, month, diaryTexts);

        // 5. Recap 엔티티 생성 및 하이라이트 추가
        Recap recap = Recap.builder()
                .userId(request.getUserId())
                .petId(request.getPetId())
                .title(aiData.getTitle())
                .summary(aiData.getSummary())
                .periodStart(request.getPeriodStart())
                .periodEnd(request.getPeriodEnd())
                .momentCount(diaries.size())
                .status(RecapStatus.GENERATED)
                .build();

        if (aiData.getHighlights() != null) {
            aiData.getHighlights().forEach(h -> {
                recap.addHighlight(RecapHighlight.builder()
                        .title(h.getTitle())
                        .content(h.getContent())
                        .build());
            });
        }

        Recap savedRecap = recapRepository.save(recap);
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