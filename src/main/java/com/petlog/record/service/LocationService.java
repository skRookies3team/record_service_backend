package com.petlog.record.service;

import com.petlog.record.dto.request.LocationRequest;
import com.petlog.record.dto.response.LocationResponse;

import java.time.LocalDate;

/**
 * [위치 정보 관리 서비스 인터페이스]
 * 실시간 산책 경로 수집 및 일기 작성을 위한 좌표 복원을 담당
 * PostGIS 기반의 지리 정보 시스템(GIS)을 활용하여 공간 데이터를 처리함
 */
public interface LocationService {

    /**
     * [대표 위치 정보 조회]
     * 특정 날짜의 이동 경로(WalkRoute) 데이터 중 일기 작성에 적합한 대표 지점(예: 시작점)을 추출
     * 과거 날짜의 일기 작성 시, 사용자가 어디에 있었는지 자동으로 복원하는 기능의 핵심 소스
     * @param userId 사용자 식별자
     * @param date 조회 대상 날짜
     * @return 위도/경도 정보를 담은 DTO (기록 부재 시 null)
     */
    LocationResponse getRepresentativeLocation(Long userId, LocalDate date);

    /**
     * [실시간 위치 데이터 적재]
     * 모바일 기기로부터 전송되는 실시간 좌표 정보를 산책 경로 데이터로 저장
     * @param request 위경도 정보를 포함한 위치 저장 요청 객체
     */
    void saveLocation(LocationRequest request);

    /**
     * [일기 기반 위치 정보 보정]
     * 과거 날짜의 일기를 저장할 때, 해당 시점의 위치 정보를 명시적으로 기록하여
     * 차후 지도 보기 및 경로 분석의 정확도를 향상시킴
     */
    void saveLocation(Long userId, LocalDate date, Double latitude, Double longitude, String locationName);
}