package com.petlog.record.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.locationtech.jts.geom.Point;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * [산책 경로 및 위치 정보 엔티티]
 * PostGIS의 공간 데이터 타입(Point)을 활용하여 사용자의 이동 지점을 저장
 * 실시간 위치 트래킹 데이터를 축적하여 산책 경로 분석 및 장소 추천의 기초 데이터로 활용
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

    /** 위치 정보를 기록한 사용자 식별 ID */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** * [공간 데이터: 시작 지점]
     * SRID 4326(WGS84) 좌표계를 사용하는 GPS 포인트 데이터
     */
    @Column(name = "start_point", columnDefinition = "geometry(Point, 4326)")
    private Point startPoint;

    /** [공간 데이터: 종료 지점] 이동 경로의 끝점 또는 특정 구간의 종료 좌표 */
    @Column(name = "end_point", columnDefinition = "geometry(Point, 4326)")
    private Point endPoint;

    /** * [기록 기준 날짜]
     * 실제 위치가 수집된 날짜. 일기 작성 시 해당 날짜의 이동 경로를 그룹화하는 기준
     */
    @Column(name = "recorded_date")
    private LocalDate recordedDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}