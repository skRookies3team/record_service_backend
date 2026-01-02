package com.petlog.record.controller;

import com.petlog.record.service.WeatherService;
import com.petlog.record.util.LatXLngY;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/diaries")
@RequiredArgsConstructor
public class WeatherController {
    
    private final WeatherService weatherService;
    
    @GetMapping("/weather")
    public ResponseEntity<Map<String, String>> getWeather(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam String date
    ) {
        // Grid 변환
        int[] grid = LatXLngY.convert(latitude, longitude);
        
        // 날짜 파싱
        LocalDate requestDate = LocalDate.parse(date);
        LocalDate today = LocalDate.now();
        
        String weather;
        if (requestDate.isBefore(today)) {
            // 과거 날씨
            weather = weatherService.getPastWeather(requestDate, latitude, longitude);
        } else {
            // 현재/미래 날씨
            weather = weatherService.getCurrentWeather(grid[0], grid[1]);
        }
        
        return ResponseEntity.ok(Map.of("weather", weather));
    }
}