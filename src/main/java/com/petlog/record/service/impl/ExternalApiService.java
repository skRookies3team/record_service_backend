package com.petlog.record.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

/**
 * 외부 API 전담 서비스
 * 날씨 조회 및 위치 주소 변환 기능 담당
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalApiService {

    private final WeatherService weatherService;
    private final RestTemplate restTemplate;

    @Value("${kakao.rest-api-key}")
    private String kakaoRestApiKey;

    public String getCurrentWeather(Double lat, Double lng) {
        try {
            int[] grid = LatXLngY.convert(lat, lng);
            return weatherService.getCurrentWeather(grid[0], grid[1]);
        } catch (Exception e) {
            log.warn("날씨 조회 실패: {}", e.getMessage());
            return null;
        }
    }

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