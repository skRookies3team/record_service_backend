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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class WeatherServiceImpl implements WeatherService {

    @Value("${external.weather.api-key}")
    private String serviceKey;  // 단기예보용

    @Value("${external.weather.asos-api-key:${external.weather.api-key}}") // ASOS용 (설정 없으면 기본 키 사용)
    private String asosServiceKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 1. 초단기예보 API (현재 날씨용)
    private static final String FCST_API_URL = "http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getUltraSrtFcst";

    // 2. 종관기상관측(ASOS) 일자료 API (과거 날씨용)
    // 활용신청 필요: https://www.data.go.kr/data/15059093/openapi.do
    private static final String ASOS_API_URL = "http://apis.data.go.kr/1360000/AsosDalyInfoService/getWthrDataList";

    // 주요 관측소 목록 (ID, 위도, 경도) - 간소화를 위해 주요 도시만 등록
    // 실제로는 DB에 전체 관측소 데이터를 넣고 쿼리하는 것이 좋음
    private static final Map<Integer, double[]> STATIONS = new HashMap<>();
    static {
        STATIONS.put(108, new double[]{37.5714, 126.9658}); // 서울
        STATIONS.put(112, new double[]{37.4527, 126.7073}); // 인천
        STATIONS.put(119, new double[]{37.2574, 127.0219}); // 수원
        STATIONS.put(98,  new double[]{37.9026, 127.0607}); // 동두천
        STATIONS.put(101, new double[]{37.8858, 127.7306}); // 춘천
        STATIONS.put(105, new double[]{37.7515, 128.8910}); // 강릉
        STATIONS.put(131, new double[]{36.6392, 127.4407}); // 청주
        STATIONS.put(133, new double[]{36.3720, 127.3721}); // 대전
        STATIONS.put(143, new double[]{35.8779, 128.6014}); // 대구
        STATIONS.put(146, new double[]{35.8215, 127.1550}); // 전주
        STATIONS.put(156, new double[]{35.1729, 126.8916}); // 광주
        STATIONS.put(159, new double[]{35.1047, 129.0324}); // 부산
        STATIONS.put(184, new double[]{33.5141, 126.5297}); // 제주
        // ... 필요 시 더 추가
    }

    @Override
    public String getCurrentWeather(int nx, int ny) {
        // ... (기존 코드와 동일: 초단기예보 호출 로직) ...
        try {
            LocalDateTime now = LocalDateTime.now();
            if (now.getMinute() < 45) {
                now = now.minusHours(1);
            }
            String baseDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String baseTime = now.format(DateTimeFormatter.ofPattern("HH30"));

            URI uri = UriComponentsBuilder.fromHttpUrl(FCST_API_URL)
                    .queryParam("serviceKey", serviceKey)
                    .queryParam("pageNo", 1)
                    .queryParam("numOfRows", 60)
                    .queryParam("dataType", "JSON")
                    .queryParam("base_date", baseDate)
                    .queryParam("base_time", baseTime)
                    .queryParam("nx", nx)
                    .queryParam("ny", ny)
                    .build(false)
                    .toUri();

            String response = restTemplate.getForObject(uri, String.class);
            return parseFcstResponse(response);
        } catch (Exception e) {
            log.error("Failed to get current weather", e);
            return "맑음";
        }
    }

    @Override
    public String getPastWeather(LocalDate date, double lat, double lng) {
        try {
            // 1. 가장 가까운 관측소 찾기
            int stationId = findNearestStation(lat, lng);
            String dateStr = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

            // 2. ASOS API 호출
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
                    .build(false)
                    .toUri();

            log.info("ASOS API Request: Date={}, Station={}, URL={}", date, stationId, uri);

            String response = restTemplate.getForObject(uri, String.class);
            return parseAsosResponse(response);

        } catch (Exception e) {
            log.error("Failed to get past weather", e);
            return "맑음"; // 실패 시 기본값
        }
    }

    // --- Helper Methods ---

    // 위경도와 가장 가까운 관측소 ID 반환 (유클리드 거리 단순 계산)
    private int findNearestStation(double lat, double lng) {
        int nearestId = 108; // 기본값: 서울
        double minDistance = Double.MAX_VALUE;

        for (Map.Entry<Integer, double[]> entry : STATIONS.entrySet()) {
            double[] coords = entry.getValue();
            double dist = Math.pow(lat - coords[0], 2) + Math.pow(lng - coords[1], 2);
            if (dist < minDistance) {
                minDistance = dist;
                nearestId = entry.getKey();
            }
        }
        return nearestId;
    }

    // 초단기예보 파싱 (기존 로직 유지)
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

    // ASOS 파싱
    private String parseAsosResponse(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode items = root.path("response").path("body").path("items").path("item");

            if (items.isArray() && items.size() > 0) {
                JsonNode item = items.get(0);
                // 강수량 확인 (sumRn: 일강수량)
                String sumRn = item.path("sumRn").asText(); // 비가 안 왔으면 "", "0.0", 또는 null

                // 전운량(avgTca) 등으로 흐림 판단 가능하지만, 간단히 강수 여부만 체크하거나
                // "iscs" (일기현상) 필드를 파싱해서 "비", "눈", "박무" 등을 찾을 수도 있음.

                if (sumRn != null && !sumRn.isEmpty() && !"0.0".equals(sumRn)) {
                    return "비"; // 강수량이 있으면 비로 간주 (눈 구분은 iscs 파싱 필요)
                }

                // 평균 전운량(avgTca): 0~2 맑음, 3~5 구름많음, 6~10 흐림
                double avgTca = item.path("avgTca").asDouble(0.0);
                if (avgTca >= 6.0) return "흐림";
                if (avgTca >= 3.0) return "구름많음";

                return "맑음";
            }
            return "맑음";
        } catch (Exception e) {
            log.error("ASOS Parsing Error", e);
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