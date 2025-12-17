package com.petlog.record.service.impl;

import com.petlog.record.dto.response.LocationResponse;
import com.petlog.record.repository.LocationRepository;
import com.petlog.record.service.LocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocationServiceImpl implements LocationService {

    private final LocationRepository locationRepository;

    @Override
    public LocationResponse getRepresentativeLocation(Long userId, LocalDate date) {
        // 1. 해당 날짜, 해당 유저의 산책 기록 중 첫 번째 위치(Point)를 조회
        Point point = locationRepository.findFirstLocationByUserIdAndDate(userId, date);

        if (point != null) {
            log.info("과거 위치 데이터 발견: Lat={}, Lng={}", point.getY(), point.getX());

            // Map 대신 DTO 빌더 패턴 사용
            return LocationResponse.builder()
                    .latitude(point.getY())  // 위도
                    .longitude(point.getX()) // 경도
                    .build();
        }

        log.warn("해당 날짜({})의 위치 기록이 없습니다.", date);
        return null;
    }
}