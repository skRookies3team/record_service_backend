package com.petlog.record.service;

import com.petlog.record.dto.response.LocationResponse; // DTO import

import java.time.LocalDate;

public interface LocationService {
    /**
     * 특정 날짜의 사용자 이동 기록 중 대표 위치(시작점 등)를 조회합니다.
     * @param userId 사용자 ID
     * @param date 조회할 날짜
     * @return 위도(latitude), 경도(longitude)를 담은 DTO (데이터가 없으면 null 반환)
     */
    LocationResponse getRepresentativeLocation(Long userId, LocalDate date);
}