package com.petlog.record.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlog.record.service.WeatherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class WeatherServiceImpl implements WeatherService {

    @Value("${external.weather.api-key}")
    private String serviceKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 기상청 초단기예보 API URL
    private static final String API_URL = "http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getUltraSrtFcst";

    @Override
    public String getCurrentWeather(int nx, int ny) {
        try {
            // 1. 기준 날짜/시간 계산 (매시 45분 업데이트)
            LocalDateTime now = LocalDateTime.now();
            if (now.getMinute() < 45) {
                now = now.minusHours(1);
            }
            
            String baseDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String baseTime = now.format(DateTimeFormatter.ofPattern("HH30")); // 30분으로 요청 시 안정적

            // 2. URI 생성
            URI uri = UriComponentsBuilder.fromHttpUrl(API_URL)
                    .queryParam("serviceKey", serviceKey)
                    .queryParam("pageNo", 1)
                    .queryParam("numOfRows", 60)
                    .queryParam("dataType", "JSON")
                    .queryParam("base_date", baseDate)
                    .queryParam("base_time", baseTime)
                    .queryParam("nx", nx)
                    .queryParam("ny", ny)
                    .build(false) // serviceKey 특수문자 인코딩 방지
                    .toUri();

            log.info("Weather API Request: nx={}, ny={}, time={}", nx, ny, baseTime);

            // 3. 호출
            String response = restTemplate.getForObject(uri, String.class);
            
            // 4. 파싱 및 결과 반환
            return parseWeatherResponse(response);

        } catch (Exception e) {
            log.error("Failed to fetch weather data: {}", e.getMessage());
            return "맑음"; // 에러 발생 시 기본값 반환
        }
    }

    private String parseWeatherResponse(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            String resultCode = root.path("response").path("header").path("resultCode").asText();
            
            if (!"00".equals(resultCode)) {
                log.warn("Weather API returned error code: {}", resultCode);
                return "맑음";
            }

            JsonNode items = root.path("response").path("body").path("items").path("item");
            if (items.isMissingNode()) {
                return "맑음";
            }

            String pty = null;
            String sky = null;

            for (JsonNode item : items) {
                String category = item.path("category").asText();
                String fcstValue = item.path("fcstValue").asText();

                if ("PTY".equals(category) && pty == null) {
                    pty = fcstValue;
                }
                if ("SKY".equals(category) && sky == null) {
                    sky = fcstValue;
                }

                if (pty != null && sky != null) break;
            }

            return mapWeatherCode(pty, sky);

        } catch (Exception e) {
            log.error("JSON Parsing Error", e);
            return "맑음";
        }
    }

    private String mapWeatherCode(String pty, String sky) {
        // 1. 강수 형태(PTY)가 있다면 그것을 우선시
        if (pty != null && !"0".equals(pty)) {
            switch (pty) {
                case "1": case "5": return "비";
                case "2": case "6": return "진눈깨비";
                case "3": case "7": return "눈";
                default: return "흐림";
            }
        }

        // 2. 강수가 없다면 하늘 상태(SKY) 반환
        if (sky != null) {
            switch (sky) {
                case "1": return "맑음";
                case "3": return "구름 조금";
                case "4": return "흐림";
                default: return "맑음";
            }
        }

        return "맑음";
    }
}