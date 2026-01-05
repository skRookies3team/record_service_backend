package com.petlog.record.repository.jpa;

import com.petlog.record.entity.WeatherStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WeatherStationRepository extends JpaRepository<WeatherStation, Integer> {

    /**
     * 특정 위경도에서 가장 가까운 관측소 하나를 반환합니다.
     * ST_DistanceSphere: 구면 모델을 기준으로 미터 단위 거리를 계산하여 정렬합니다.
     */
    @Query(value = """
        SELECT * FROM weather_stations s 
        ORDER BY ST_DistanceSphere(s.location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)) 
        LIMIT 1
    """, nativeQuery = true)
    Optional<WeatherStation> findNearestStation(@Param("lat") double lat, @Param("lng") double lng);
}