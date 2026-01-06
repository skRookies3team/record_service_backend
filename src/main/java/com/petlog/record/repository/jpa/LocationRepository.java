package com.petlog.record.repository.jpa;

import com.petlog.record.entity.WalkRoute;
import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

/**
 * [위치 정보 관리 리포지토리]
 * PostGIS의 공간 연산을 활용하여 이동 경로 중 특정 시점의 좌표를 추출
 */
@Repository
public interface LocationRepository extends JpaRepository<WalkRoute, Long> {

    /**
     * [대표 위치 조회]
     * 특정 날짜에 수집된 위치 데이터 중 가장 최근 지점을 네이티브 쿼리로 조회
     * @param date recorded_date 필드 기준의 기록 날짜
     */
    @Query(value = """
        SELECT w.start_point 
        FROM walk_routes w 
        WHERE w.user_id = :userId 
          AND w.recorded_date = :date
        ORDER BY w.created_at DESC 
        LIMIT 1
    """, nativeQuery = true)
    Point findFirstLocationByUserIdAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);
}