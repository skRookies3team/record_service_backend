package com.petlog.record.service;

import java.time.LocalDate;

/**
 * [외부 API 데이터 통합 서비스 인터페이스]
 * 기상청 및 지도 API를 연동하여 위치 기반의 주소와 날씨 정보를 수집
 */
public interface ExternalApiService {
    /** [실시간 날씨] 현재 좌표 기준의 단기 예보 정보 획득 */
    String getCurrentWeather(Double lat, Double lng);

    /** [과거 날씨] 특정 날짜의 관측 데이터 기반 기상 상태 획득 */
    String getPastWeather(LocalDate date, Double lat, Double lng);

    /** [통합 날씨 조회] 날짜 정보에 따라 과거/현재 API를 분기하여 호출 */
    String getWeatherInfo(LocalDate date, Double lat, Double lng);

    /** [역지오코딩] 위경도 좌표를 행정구역 단위 주소 명칭으로 변환 */
    String getAddressFromCoords(Double lat, Double lng);
}