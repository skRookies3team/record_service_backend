package com.petlog.record.controller;

import com.petlog.record.service.WeatherService;
import com.petlog.record.util.LatXLngY;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/diaries")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherService weatherService;

    @GetMapping("/weather")
    public ResponseEntity<Map<String, String>> getWeather(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam String date
    ) {
        try {
            // 1. 날짜 파싱 및 오늘 날짜 비교
            LocalDate requestDate = LocalDate.parse(date);
            LocalDate today = LocalDate.now();

            String weather;
            if (requestDate.isBefore(today)) {
                // 2. 과거 날씨 (PostGIS를 통해 관측소 찾고 ASOS API 호출)
                log.info("조회 모드: 과거 날씨 (Date: {}, Lat: {}, Lng: {})", date, latitude, longitude);
                weather = weatherService.getPastWeather(requestDate, latitude, longitude);
            } else {
                // 3. 현재/미래 날씨 (Grid 변환 후 단기예보 API 호출)
                log.info("조회 모드: 현재 날씨 (Date: {}, Lat: {}, Lng: {})", date, latitude, longitude);
                int[] grid = LatXLngY.convert(latitude, longitude);
                weather = weatherService.getCurrentWeather(grid[0], grid[1]);
            }

            return ResponseEntity.ok(Map.of("weather", weather));

        } catch (DateTimeParseException e) {
            log.error("잘못된 날짜 형식: {}", date);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("날씨 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("weather", "맑은날씨")); // 에러 시 기본값 반환
        }
    }
}