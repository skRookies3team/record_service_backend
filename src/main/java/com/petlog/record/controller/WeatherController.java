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

/**
 * [날씨 정보 조회 컨트롤러]
 * 외부 기상 API를 연동하여 특정 위치와 날짜에 해당하는 날씨 데이터를 제공하는 컨트롤러
 * 일기 생성 시 '날씨 자동 입력' 기능을 지원하기 위해 사용됨
 */
@Slf4j
@RestController
@RequestMapping("/api/diaries")
@RequiredArgsConstructor
public class WeatherController {

    private final ExternalApiService externalApiService;

    /**
     * [위치 및 날짜 기반 날씨 조회 API]
     * 위도, 경도, 날짜 정보를 바탕으로 과거 기록 또는 현재의 기상 정보를 조회
     * 서비스 계층(ExternalApiService)에서 날짜에 따른 과거/현재 날씨 조회 분기 로직을 처리
     * * @param latitude 위도
     * @param longitude 경도
     * @param date 조회하고자 하는 날짜 (YYYY-MM-DD)
     * @return 날씨 정보 (예: "맑음", "흐림", "비" 등)
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
            /* * [Fallback 전략]
             * 외부 API 장애나 알 수 없는 오류 발생 시에도 사용자 흐름을 방해하지 않기 위해
             * 로그만 남기고 기본값("맑음")을 반환하도록 처리
             */
            log.error("Error occurred while fetching weather info: {}", e.getMessage(), e);
            return ResponseEntity.ok(Map.of("weather", "맑음"));
        }
    }
}