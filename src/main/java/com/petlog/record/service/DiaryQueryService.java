package com.petlog.record.service;

import com.petlog.record.dto.response.DiaryResponse;

import java.time.LocalDate;
import java.util.List;

/**
 * [다이어리 조회 전담 서비스 인터페이스]
 * CQRS(Command Query Responsibility Segregation) 패턴에 따라
 * 상태를 변경하지 않는 읽기 전용 작업(캘린더, 보관함 조회 등)의 명세를 정의
 */
public interface DiaryQueryService {

    /**
     * [캘린더 날짜별 일기 조회]
     * 사용자가 캘린더 화면에서 특정 날짜를 선택했을 때 해당 일의 일기 목록을 제공
     * @param userId 사용자 식별자
     * @param date 조회 대상 날짜 (YYYY-MM-DD)
     * @return 다이어리 상세 정보 리스트
     */
    List<DiaryResponse> getDiariesByDate(Long userId, LocalDate date);

    /**
     * [AI 다이어리 보관함 전체 조회]
     * 사용자가 AI 초안 기능을 사용하여 생성한 모든 일기 목록을 조회
     * @param userId 사용자 식별자
     * @return AI 생성 다이어리 리스트 (최신순)
     */
    List<DiaryResponse> getAiDiaries(Long userId);
}