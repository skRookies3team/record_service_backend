package com.petlog.record.service.impl;

import com.petlog.record.dto.response.DiaryResponse;
import com.petlog.record.entity.Diary;
import com.petlog.record.repository.jpa.DiaryQueryRepository;
import com.petlog.record.service.DiaryQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * [기록 조회 서비스 구현체]
 * 다이어리 데이터를 캘린더, 보관함 등 다양한 뷰에 맞춰 필터링 및 변환하여 제공하는 서비스
 * CQRS 패턴을 고려하여 조회 성능 최적화를 위해 읽기 전용 트랜잭션 정책을 따름
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryQueryServiceImpl implements DiaryQueryService {

    private final DiaryQueryRepository diaryQueryRepository;

    /**
     * [날짜별 다이어리 목록 조회]
     * 특정 사용자가 선택한 날짜에 기록한 일기들을 조회
     * 주로 모바일 앱의 캘린더 화면에서 특정 날짜를 터치했을 때 해당 일의 목록을 구성하기 위해 사용
     * * @param userId 사용자 식별자
     * @param date 조회 대상 날짜 (LocalDate)
     * @return 다이어리 응답 DTO 리스트
     */
    @Override
    public List<DiaryResponse> getDiariesByDate(Long userId, LocalDate date) {
        // 리포지토리를 통해 날짜 기반 데이터 조회
        List<Diary> diaries = diaryQueryRepository.findAllByUserIdAndDate(userId, date);

        // 엔티티 리스트를 프론트엔드 응답용 DTO 리스트로 변환
        return diaries.stream()
                .map(DiaryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * [AI 생성 다이어리 보관함 조회]
     * 사용자가 AI 초안 생성 기능을 사용하여 작성한 일기들만 별도로 모아서 조회
     * AI 기록들만 따로 모아보고 싶은 보관함 탭 UI에서 호출됨
     * 최신순(CreatedAt 역순)으로 정렬하여 반환
     * * @param userId 사용자 식별자
     * @return AI 생성 다이어리 응답 DTO 리스트
     */
    @Override
    public List<DiaryResponse> getAiDiaries(Long userId) {
        // isAiGen 플래그가 true인 데이터만 최신순으로 조회
        List<Diary> aiDiaries = diaryQueryRepository.findAllByUserIdAndIsAiGenOrderByCreatedAtDesc(userId, true);

        return aiDiaries.stream()
                .map(DiaryResponse::fromEntity)
                .collect(Collectors.toList());
    }
}