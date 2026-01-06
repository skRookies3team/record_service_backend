package com.petlog.record.service;

import java.time.LocalDate;

/**
 * [기상 정보 통합 연동 서비스 인터페이스]
 * 기상청의 실시간 예보(Forecast)와 과거 관측(Observation) 데이터를 통합 관리
 * 위치(격자 좌표) 기반의 기상 상태 정보를 추출하여 일기 데이터에 주입함
 */
public interface WeatherService {

    /**
     * [현재 날씨 조회]
     * 기상청 단기예보(격자 좌표 nx, ny 기반)를 호출하여
     * 현재 시점의 날씨 상태(맑음, 비, 눈 등)를 반환
     */
    String getCurrentWeather(int nx, int ny);

    /**
     * [과거 기상 기록 조회]
     * 특정 과거 날짜의 위경도 좌표를 기준으로 가장 가까운 관측소(ASOS) 데이터를 조회
     * @param date 관측 대상 날짜
     * @param lat 위도
     * @param lng 경도
     * @return 분석된 과거 날씨 명칭
     */
    String getPastWeather(LocalDate date, double lat, double lng);
}