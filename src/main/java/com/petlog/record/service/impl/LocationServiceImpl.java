package com.petlog.record.service.impl;

import com.petlog.record.dto.request.LocationRequest;
import com.petlog.record.dto.response.LocationResponse;
import com.petlog.record.entity.WalkRoute;
import com.petlog.record.repository.jpa.LocationRepository;
import com.petlog.record.service.LocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * [위치 정보 및 경로 서비스 구현체]
 * PostGIS와 JTS(Java Topology Suite)를 활용하여 지리 공간 데이터를 관리
 * 표준 GPS 좌표계(WGS84, SRID: 4326)를 기반으로 위치를 저장하고 조회함
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocationServiceImpl implements LocationService {

    private final LocationRepository locationRepository;

    /** * PostGIS 좌표 생성을 위한 팩토리
     * SRID 4326: 전 지구적 위치 파악을 위한 WGS84 좌표계 설정
     */
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    /**
     * [대표 위치 정보 조회]
     * 특정 날짜에 기록된 사용자의 위치 이력 중 가장 적합한 지점을 반환
     * @param userId 사용자 고유 식별자
     * @param date 조회 대상 날짜
     * @return 좌표(위도, 경도)를 담은 DTO, 기록이 없을 시 null 반환
     */
    @Override
    public LocationResponse getRepresentativeLocation(Long userId, LocalDate date) {
        Point point = locationRepository.findFirstLocationByUserIdAndDate(userId, date);

        if (point != null) {
            return LocationResponse.builder()
                    .latitude(point.getY())
                    .longitude(point.getX())
                    .build();
        }
        return null;
    }

    /**
     * [실시간 위치 추적 데이터 저장]
     * 클라이언트로부터 전송받은 실시간 좌표를 산책 경로(WalkRoute)로 기록
     * ⚠️ 주의: JTS Coordinate 생성 시 (x, y) 순서이므로 (Longitude, Latitude)로 매핑해야 함
     */
    @Override
    @Transactional
    public void saveLocation(LocationRequest request) {
        // 1. 위도/경도로 Point 객체 생성 (순서: 경도(x), 위도(y))
        // 주의: Google Map 등은 (Lat, Lng) 순서지만, PostGIS/JTS는 (x, y) = (Lng, Lat) 순서입니다.
        Point point = geometryFactory.createPoint(new Coordinate(request.getLongitude(), request.getLatitude()));

        // 2. 엔티티 생성 (현재 시간을 created_at으로 자동 저장)
        WalkRoute walkRoute = WalkRoute.builder()
                .userId(request.getUserId())
                .startPoint(point)
                .build();

        // 3. 저장
        locationRepository.save(walkRoute);
        log.info("DB 저장 완료 (WalkRoute ID: {})", walkRoute.getId());
    }

    /**
     * [과거 일기용 위치 데이터 수동 저장]
     * 특정 과거 날짜의 일기를 저장할 때, 해당 시점의 위치 정보를 명시적으로 기록
     */
    @Override
    @Transactional
    public void saveLocation(Long userId, LocalDate date, Double latitude, Double longitude, String locationName) {
        Point point = geometryFactory.createPoint(new Coordinate(longitude, latitude));

        WalkRoute walkRoute = WalkRoute.builder()
                .userId(userId)
                .startPoint(point)
                .recordedDate(date) // 일기 날짜 저장
                .build();

        locationRepository.save(walkRoute);
        log.info("과거 일기 위치 저장 완료: userId={}, date={}, lat={}, lng={}", userId, date, latitude, longitude);
    }
}