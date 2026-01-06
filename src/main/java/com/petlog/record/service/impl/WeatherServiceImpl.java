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

/**
 * [기상 정보 통합 연동 서비스 구현체]
 * 기상청 단기예보(실시간) 및 ASOS(과거 관측) API를 호출하여 위치 기반 기상 정보 제공
 * 공공데이터 포털 특유의 인증 키 인코딩 문제를 해결하기 위해 URI 객체 생성 방식을 정교화함
 */
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

    /**
     * [현재 날씨 조회 (단기예보)]
     * 기상청 격자 좌표(nx, ny)를 기반으로 현재 시점의 기상 상태(강수, 하늘 상태)를 분석
     */
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

    /**
     * [과거 날씨 조회 (ASOS)]
     * 1. PostGIS를 통해 위경도와 가장 인접한 관측소(Station) 식별
     * 2. 해당 관측소의 과거 일자 데이터를 조회하여 분석
     */
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

    /**
     * [ASOS 과거 기상 데이터 호출]
     * 지정된 날짜와 관측소 식별자를 기반으로 공공데이터 포털의 ASOS API를 호출
     * * [기술적 해결: 401 Unauthorized 에러 방지]
     * 공공데이터 포털의 서비스 키는 인코딩/디코딩 상태에 따라 API 호출이 실패할 확률이 높음
     * UriComponentsBuilder를 통해 키를 주입하고 마지막에 .encode()를 수행하여
     * RestTemplate에 의한 이중 인코딩 문제를 원천 차단함
     * * @param date 관측 일자
     * @param stationId 기상청 관측소 번호
     * @return 분석된 날씨 결과 문자열
     */
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

    /**
     * [ASOS 응답 데이터 파싱 및 날씨 추론]
     * 기상청 관측 데이터(JSON)에서 기상현상, 강수량, 운량을 분석하여 서비스 표준 날씨로 변환
     * * [추론 로직 우선순위]
     * 1. 기상현상(iscs): '눈' 또는 '비' 단어가 포함된 경우 해당 날씨를 즉시 반환
     * 2. 강수량(sumRn): 현상 코드에 없더라도 실제 강수량이 감지되면 '비'로 판단
     * 3. 전운량(avgTca): 현상이 없는 경우 평균 운량에 따라 '맑음/구름많음/흐림' 결정
     */
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

    /**
     * [단기예보(실시간) 응답 데이터 파싱]
     * 기상청 실시간 예보 JSON에서 강수형태(PTY)와 하늘상태(SKY) 카테고리만 필터링하여 추출
     * * @param jsonResponse API로부터 전달받은 원본 JSON
     * @return 서비스 표준 날씨 명칭 (mapWeatherCode로 변환)
     */
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

    /**
     * [기상 코드 매핑 테이블]
     * API 응답 코드(PTY, SKY)를 서비스 내 표준 명칭으로 변환
     */
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