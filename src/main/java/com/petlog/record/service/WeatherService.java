package com.petlog.record.service;

import java.time.LocalDate;

public interface WeatherService {
    // [기존] 현재/미래 날씨 (위도, 경도 -> 격자변환 -> 초단기예보)
    String getCurrentWeather(int nx, int ny);

    // [추가] 과거 날씨 (날짜, 위도, 경도 -> 관측소 매핑 -> ASOS 일자료)
    String getPastWeather(LocalDate date, double lat, double lng);
}