package com.petlog.record.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlog.record.service.ExternalApiService;
import com.petlog.record.service.WeatherService;
import com.petlog.record.util.LatXLngY;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;

/**
 * [외부 API 통합 연동 서비스]
 * 기상청 날씨 데이터(과거/현재) 및 Kakao 로컬 API(주소 변환)를 연동하여
 * 일기 데이터에 필요한 환경 정보를 수집함
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalApiServiceImpl implements ExternalApiService {

    private final WeatherService weatherService;
    private final RestTemplate restTemplate;

    @Value("${kakao.rest-api-key}")
    private String kakaoRestApiKey;

    /**
     * [통합 날씨 정보 조회]
     * 입력된 날짜가 과거인지 오늘인지에 따라 관측 데이터(Past) 또는 예보 데이터(Current)를 선택적으로 조회
     * @param date 일기 작성 날짜
     * @param lat 위도
     * @param lng 경도
     */
    @Override
    public String getWeatherInfo(LocalDate date, Double lat, Double lng) {
        if (lat == null || lng == null) return null;

        LocalDate today = LocalDate.now();
        // 과거 날짜면 ASOS(관측), 오늘이면 단기예보를 호출합니다.
        if (date.isBefore(today)) {
            return getPastWeather(date, lat, lng);
        } else {
            return getCurrentWeather(lat, lng);
        }
    }

    /**
     * [실시간 날씨 조회]
     * 위경도 좌표를 기상청 격자 좌표(X, Y)로 변환하여 현재 날씨를 조회
     */
    @Override
    public String getCurrentWeather(Double lat, Double lng) {
        try {
            int[] grid = LatXLngY.convert(lat, lng);
            return weatherService.getCurrentWeather(grid[0], grid[1]);
        } catch (Exception e) {
            log.warn("실시간 날씨 조회 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * [과거 날씨 조회]
     * 특정 날짜의 위경도와 가장 인접한 관측소 데이터를 조회
     */
    @Override
    public String getPastWeather(LocalDate date, Double lat, Double lng) {
        try {
            return weatherService.getPastWeather(date, lat, lng);
        } catch (Exception e) {
            log.warn("과거 날씨 조회 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * [좌표 기반 주소 변환 (Reverse Geocoding)]
     * Kakao Local API를 호출하여 위경도 좌표를 행정동 단위 주소로 변환
     * 우선순위: 행정동('H') 주소를 우선적으로 반환함
     */
    @Override
    public String getAddressFromCoords(Double lat, Double lng) {
        try {
            if (kakaoRestApiKey == null || kakaoRestApiKey.isEmpty()) return null;

            String url = String.format("https://dapi.kakao.com/v2/local/geo/coord2regioncode.json?x=%s&y=%s", lng, lat);
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + kakaoRestApiKey);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());
            JsonNode documents = root.path("documents");

            if (documents.isArray() && documents.size() > 0) {
                for (JsonNode doc : documents) {
                    if ("H".equals(doc.path("region_type").asText())) {
                        return doc.path("address_name").asText();
                    }
                }
                return documents.get(0).path("address_name").asText();
            }
        } catch (Exception e) {
            log.error("Kakao API 주소 변환 실패: {}", e.getMessage());
        }
        return null;
    }
}