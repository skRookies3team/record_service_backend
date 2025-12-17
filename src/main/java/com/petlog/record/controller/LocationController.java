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

@Slf4j
@Tag(name = "Location API", description = "위치 정보 및 이력 조회 API")
@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173") // [중요] CORS 허용 추가
public class LocationController {

    private final LocationService locationService;

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

    // [NEW] 실시간 위치 저장 (프론트엔드 LocationTracker가 20분마다 호출)
    @Operation(summary = "실시간 위치 저장", description = "앱 사용 중 현재 위치를 DB에 저장합니다.")
    @PostMapping
    public ResponseEntity<Void> saveLocation(@RequestBody @Valid LocationRequest request) {
        log.info("위치 저장 요청 수신 - User: {}, Lat: {}, Lng: {}", request.getUserId(), request.getLatitude(), request.getLongitude());
        locationService.saveLocation(request);
        return ResponseEntity.ok().build();
    }
}