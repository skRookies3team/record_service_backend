package com.petlog.record.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

/**
 * 기상청 종관기상관측(ASOS) 관측소 정보 엔티티
 */
@Entity
@Table(name = "weather_stations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WeatherStation {

    @Id
    @Column(name = "station_id")
    private Integer id; // 기상청 관측소 번호 (예: 108)

    private String name; // 관측소 명칭 (예: 서울)

    // PostGIS Point 객체 (SRID 4326)
    @Column(columnDefinition = "geometry(Point, 4326)")
    private Point location;

    @Builder
    public WeatherStation(Integer id, String name, Point location) {
        this.id = id;
        this.name = name;
        this.location = location;
    }
}