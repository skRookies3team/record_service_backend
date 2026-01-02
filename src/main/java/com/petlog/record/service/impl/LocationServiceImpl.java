package com.petlog.record.service.impl;

import com.petlog.record.dto.request.LocationRequest;
import com.petlog.record.dto.response.LocationResponse;
import com.petlog.record.entity.WalkRoute;
import com.petlog.record.repository.jpa.LocationRepository;
import com.petlog.record.service.LocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocationServiceImpl implements LocationService {

    private final LocationRepository locationRepository;
    // PostGIS 좌표 생성을 위한 팩토리 (SRID 4326: WGS84 - GPS 좌표계)
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

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
}