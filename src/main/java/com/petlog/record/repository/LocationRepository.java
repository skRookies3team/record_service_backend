package com.petlog.record.repository;

import com.petlog.record.entity.WalkRoute;
import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface LocationRepository extends JpaRepository<WalkRoute, Long> {

    // [Query 수정]
    // 날짜를 TIMESTAMP로 캐스팅하여 정확한 24시간 범위 내의 데이터를 조회합니다.
    // 범위: 해당 날짜 00:00:00 <= created_at < 다음 날짜 00:00:00
    @Query(value = """
        SELECT w.start_point 
        FROM walk_routes w 
        WHERE w.user_id = :userId 
          AND w.created_at >= CAST(:date AS timestamp)
          AND w.created_at < CAST(:date AS timestamp) + INTERVAL '1 day'
        ORDER BY w.created_at ASC 
        LIMIT 1
    """, nativeQuery = true)
    Point findFirstLocationByUserIdAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);
}