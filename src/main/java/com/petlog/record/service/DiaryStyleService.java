package com.petlog.record.service;

import com.petlog.record.dto.request.DiaryStyleRequest;
import com.petlog.record.dto.response.DiaryStyleResponse;

/**
 * [다이어리 스타일 관리 서비스 인터페이스]
 * 사용자가 설정한 레이아웃, 폰트, 테마 등 시각적 요소를 관리
 * 유저 기본 -> 펫 전용 -> 개별 일기 순의 스타일 오버라이딩 정책을 지원함
 */
public interface DiaryStyleService {

    /**
     * [스타일 설정 생성 및 갱신]
     * 특정 대상(유저/펫/일기)에 대한 스타일 설정을 저장하거나 기존 설정을 업데이트
     * @param userId 요청자 ID
     * @param request 스타일 상세 설정 정보
     */
    DiaryStyleResponse createOrUpdateStyle(Long userId, DiaryStyleRequest request);

    /** [스타일 식별자 기반 수정] 고유 ID를 통한 특정 스타일 설정값 변경 */
    DiaryStyleResponse updateStyle(Long styleId, DiaryStyleRequest request, Long userId);

    /** [사용자/펫 기본 스타일 조회] 설정 부재 시 시스템 기본값을 생성하여 반환하는 Fallback 로직 포함 */
    DiaryStyleResponse getUserStyle(Long userId, Long petId);

    /** [펫 전용 스타일 조회] 특정 반려동물 프로필에 연결된 UI 테마 정보 반환 */
    DiaryStyleResponse getPetStyle(Long petId, Long userId);

    /** [개별 일기 스타일 조회] 특정 일기(Diary)에만 단독으로 적용된 커스텀 스타일 조회 */
    DiaryStyleResponse getDiaryStyle(Long diaryId);
}