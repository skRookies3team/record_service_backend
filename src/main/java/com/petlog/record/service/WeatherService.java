package com.petlog.record.service;

public interface WeatherService {
    // 위도(nx), 경도(ny)를 입력받아 날씨 상태(맑음, 흐림, 비 등)를 반환
    String getCurrentWeather(int nx, int ny);
}