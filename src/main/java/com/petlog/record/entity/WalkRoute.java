package com.petlog.record.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;

/**
 * 산책 경로 및 위치 정보를 저장하는 엔티티
 * PostGIS의 Point 타입을 사용하여 좌표를 저장합니다.
 */
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "walk_routes")
public class WalkRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 사용자 ID
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 산책 시작 지점 (위도/경도)
    // SRID 4326은 WGS84 좌표계(GPS)를 의미합니다.
    @Column(name = "start_point", columnDefinition = "geometry(Point, 4326)")
    private Point startPoint;

    // 산책 종료 지점 (필요 시 사용)
    @Column(name = "end_point", columnDefinition = "geometry(Point, 4326)")
    private Point endPoint;

    // 전체 경로 (LineString 등으로 확장 가능)
    // @Column(columnDefinition = "geometry(LineString, 4326)")
    // private LineString path;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}