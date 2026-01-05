package com.petlog.record.service;

import java.time.LocalDate;

public interface ExternalApiService {
    String getCurrentWeather(Double lat, Double lng);
    String getPastWeather(LocalDate date, Double lat, Double lng);
    String getWeatherInfo(LocalDate date, Double lat, Double lng);
    String getAddressFromCoords(Double lat, Double lng);
}