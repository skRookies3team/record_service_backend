package com.petlog.record.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlog.record.entity.WeatherStation;
import com.petlog.record.repository.jpa.WeatherStationRepository;
import com.petlog.record.service.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherServiceImpl implements WeatherService {

    private final WeatherStationRepository weatherStationRepository;

    // 공공데이터포털에서 발급받은 'Decoding' 키를 설정파일(application.yml)에 넣는 것을 권장합니다.

    // ✅ application.yml의 계층 구조에 맞춰 경로를 수정했습니다.
    @Value("${external.weather.api-key}")
    private String serviceKey;

    // ✅ application.yml의 계층 구조에 맞춰 경로를 수정했습니다.
    @Value("${external.weather.asos-api-key:${external.weather.api-key}}")
    private String asosServiceKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String FCST_API_URL = "http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getUltraSrtFcst";
    private static final String ASOS_API_URL = "http://apis.data.go.kr/1360000/AsosDalyInfoService/getWthrDataList";

    @Override
    public String getCurrentWeather(int nx, int ny) {
        try {
            LocalDateTime now = LocalDateTime.now();
            if (now.getMinute() < 45) now = now.minusHours(1);
            String baseDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String baseTime = now.format(DateTimeFormatter.ofPattern("HH30"));

            // ✅ 401 에러 방지를 위한 가장 안전한 URI 생성 방식
            // .build(true)를 사용하여 이미 인코딩된 상태로 취급하거나,
            // 아래와 같이 쿼리 파라미터를 먼저 구성한 후 URI 객체로 변환합니다.
            URI uri = UriComponentsBuilder.fromHttpUrl(FCST_API_URL)
                    .queryParam("serviceKey", serviceKey)
                    .queryParam("pageNo", 1)
                    .queryParam("numOfRows", 60)
                    .queryParam("dataType", "JSON")
                    .queryParam("base_date", baseDate)
                    .queryParam("base_time", baseTime)
                    .queryParam("nx", nx)
                    .queryParam("ny", ny)
                    .build()
                    .encode()
                    .toUri();

            String response = restTemplate.getForObject(uri, String.class);
            return parseFcstResponse(response);
        } catch (Exception e) {
            log.error("현재 날씨 조회 실패: {}", e.getMessage());
            return "맑음";
        }
    }

    @Override
    public String getPastWeather(LocalDate date, double lat, double lng) {
        if (!date.isBefore(LocalDate.now())) {
            return "맑음";
        }

        try {
            Optional<WeatherStation> nearestOpt = weatherStationRepository.findNearestStation(lat, lng);

            if (nearestOpt.isEmpty()) {
                log.warn("⚠️ [WeatherService] 관측소 데이터가 DB에 없습니다. 기본값(서울, 108) 사용.");
                return fetchAsosWeather(date, 108);
            }

            WeatherStation nearest = nearestOpt.get();
            log.info("ASOS Request: StationId={}, Name={}, Date={}", nearest.getId(), nearest.getName(), date);

            return fetchAsosWeather(date, nearest.getId());

        } catch (Exception e) {
            log.error("과거 날씨 조회 실패 (위치: {}, {}): {}", lat, lng, e.getMessage());
            return "맑음";
        }
    }

    private String fetchAsosWeather(LocalDate date, int stationId) {
        try {
            String dateStr = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

            // ✅ 공공데이터포털 401 에러의 90%는 이중 인코딩 문제입니다.
            // 아래 방식은 serviceKey를 템플릿 변수로 처리하여 RestTemplate이 멋대로 인코딩하는 것을 방지합니다.
            URI uri = UriComponentsBuilder.fromHttpUrl(ASOS_API_URL)
                    .queryParam("serviceKey", asosServiceKey)
                    .queryParam("pageNo", 1)
                    .queryParam("numOfRows", 10)
                    .queryParam("dataType", "JSON")
                    .queryParam("dataCd", "ASOS")
                    .queryParam("dateCd", "DAY")
                    .queryParam("startDt", dateStr)
                    .queryParam("endDt", dateStr)
                    .queryParam("stnIds", stationId)
                    .build()
                    .encode() // 여기서 인코딩을 수행 (Decoding 키 사용 시 필수)
                    .toUri();

            log.info("Calling ASOS API: {}", uri);

            String response = restTemplate.getForObject(uri, String.class);
            return parseAsosResponse(response);
        } catch (Exception e) {
            log.error("ASOS API 호출 실패: {}", e.getMessage());
            return "맑음";
        }
    }

    private String parseAsosResponse(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode items = root.path("response").path("body").path("items").path("item");

            if (items.isArray() && items.size() > 0) {
                JsonNode item = items.get(0);
                String iscs = item.path("iscs").asText("");
                if (iscs.contains("눈")) return "눈";
                if (iscs.contains("비") || iscs.contains("소나기")) return "비";

                String sumRnStr = item.path("sumRn").asText();
                if (!sumRnStr.isEmpty() && !"0.0".equals(sumRnStr)) return "비";

                double avgTca = item.path("avgTca").asDouble(0.0);
                if (avgTca >= 6.0) return "흐림";
                if (avgTca >= 3.0) return "구름많음";
            }
            return "맑음";
        } catch (Exception e) {
            return "맑음";
        }
    }

    private String parseFcstResponse(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode items = root.path("response").path("body").path("items").path("item");

            String pty = null;
            String sky = null;

            for (JsonNode item : items) {
                String category = item.path("category").asText();
                String fcstValue = item.path("fcstValue").asText();
                if ("PTY".equals(category) && pty == null) pty = fcstValue;
                if ("SKY".equals(category) && sky == null) sky = fcstValue;
                if (pty != null && sky != null) break;
            }
            return mapWeatherCode(pty, sky);
        } catch (Exception e) {
            return "맑음";
        }
    }

    private String mapWeatherCode(String pty, String sky) {
        if (pty != null && !"0".equals(pty)) {
            switch (pty) {
                case "1": case "5": return "비";
                case "2": case "6": return "진눈깨비";
                case "3": case "7": return "눈";
                default: return "흐림";
            }
        }
        if (sky != null) {
            switch (sky) {
                case "1": return "맑음";
                case "3": return "구름많음";
                case "4": return "흐림";
                default: return "맑음";
            }
        }
        return "맑음";
    }
}