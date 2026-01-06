package com.petlog.record.controller;

import com.petlog.record.dto.request.LocationRequest;
import com.petlog.record.dto.response.LocationResponse;
import com.petlog.record.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * [위치 정보 및 경로 관리 컨트롤러]
 * PostGIS 공간 데이터를 활용하여 사용자의 이동 경로를 기록하고,
 * 특정 시점의 대표 위치 이력을 조회하는 기능을 제공
 */
@Slf4j
@Tag(name = "Location API", description = "위치 정보 및 이력 조회 API")
@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    /**
     * [과거 위치 이력 조회 API]
     * 특정 사용자가 선택한 날짜의 이동 기록(공간 데이터)을 분석하여 대표 위치를 반환
     * 일기 작성 시 당시의 위치를 자동으로 불러오거나, 경로 시각화의 기초 데이터로 사용됨
     * @param userId 사용자 식별자
     * @param date 조회하고자 하는 특정 날짜
     */
    @Operation(summary = "과거 위치 이력 조회", description = "특정 날짜의 사용자 이동 기록(PostGIS) 중 대표 위치를 조회합니다.")
    @GetMapping("/history")
    public ResponseEntity<LocationResponse> getLocationHistory(
            @RequestParam Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        log.info("위치 이력 조회 요청 수신 - UserId: {}, Date: {}", userId, date);

        LocationResponse location = locationService.getRepresentativeLocation(userId, date);

        if (location == null) {
            log.info("해당 날짜의 위치 기록 없음 (404 반환)");
            return ResponseEntity.notFound().build();
        }

        log.info("위치 기록 반환: {}", location);
        return ResponseEntity.ok(location);
    }

    /**
     * [실시간 위치 데이터 저장 API]
     * 모바일 앱의 LocationTracker로부터 전달받은 현재 위/경도 데이터를 DB에 저장
     * 프론트엔드에서 약 20분 간격으로 호출하며, 수집된 데이터는 추후 이동 경로 분석에 활용됨
     */
    @Operation(summary = "실시간 위치 저장", description = "앱 사용 중 현재 위치를 DB에 저장합니다.")
    @PostMapping
    public ResponseEntity<Void> saveLocation(@RequestBody @Valid LocationRequest request) {
        log.info("위치 저장 요청 수신 - User: {}, Lat: {}, Lng: {}", request.getUserId(), request.getLatitude(), request.getLongitude());
        locationService.saveLocation(request);
        return ResponseEntity.ok().build();
    }
}