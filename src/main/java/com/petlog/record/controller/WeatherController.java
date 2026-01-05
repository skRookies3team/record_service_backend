package com.petlog.record.controller;

import com.petlog.record.service.ExternalApiService;
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

    private final ExternalApiService externalApiService;

    /**
     * 날짜와 위치에 따른 날씨 정보를 조회합니다.
     * ExternalApiService 내부에서 과거/현재 날씨 분기 처리를 수행합니다.
     */
    @GetMapping("/weather")
    public ResponseEntity<Map<String, String>> getWeather(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam String date
    ) {
        try {
            LocalDate requestDate = LocalDate.parse(date);

            // 서비스 계층으로 모든 분기 로직을 위임하여 컨트롤러를 단순화했습니다.
            String weather = externalApiService.getWeatherInfo(requestDate, latitude, longitude);

            if (weather == null) weather = "맑음";

            return ResponseEntity.ok(Map.of("weather", weather));

        } catch (DateTimeParseException e) {
            log.error("Invalid date format: {}", date);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error occurred while fetching weather info: {}", e.getMessage(), e);
            return ResponseEntity.ok(Map.of("weather", "맑음"));
        }
    }
}