package com.petlog.record.repository.jpa;

import com.petlog.record.entity.WeatherStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * [기상 관측소 위치 조회 리포지토리]
 * 사용자의 위/경도 좌표와 가장 인접한 기상 관측소를 공간 연산을 통해 검색
 */
@Repository
public interface WeatherStationRepository extends JpaRepository<WeatherStation, Integer> {

     /**
     * [근접 관측소 검색]
     * ST_DistanceSphere를 사용하여 실제 지구 구면 모델 기준 미터(m) 단위로
     * 가장 가까운 관측소 1곳을 반환 (SRID 4326 좌표계 기반)
     */
    @Query(value = """
        SELECT * FROM weather_stations s 
        ORDER BY ST_DistanceSphere(s.location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)) 
        LIMIT 1
    """, nativeQuery = true)
    Optional<WeatherStation> findNearestStation(@Param("lat") double lat, @Param("lng") double lng);
}