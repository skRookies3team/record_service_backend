package com.petlog.record.service;

public interface ExternalApiService {
    String getCurrentWeather(Double lat, Double lng);
    String getAddressFromCoords(Double lat, Double lng);
}