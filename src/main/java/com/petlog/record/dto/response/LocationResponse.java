package com.petlog.record.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * [위치 정보 조회 응답 DTO]
 * PostGIS에서 계산된 특정 시점의 좌표(위도, 경도) 정보를 전달하는 객체
 * 주로 일기 기록 시 대표 위치 정보를 노출하기 위해 사용됨
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "위치 정보 응답 DTO")
public class LocationResponse {

    /** 대상 위치의 위도 (WGS84 좌표계) */
    @Schema(description = "위도", example = "37.5665")
    private Double latitude;

    /** 대상 위치의 경도 (WGS84 좌표계) */
    @Schema(description = "경도", example = "126.9780")
    private Double longitude;

    /**
     * [정적 팩토리 메서드]
     * 위도와 경도 값을 직접 받아 LocationResponse 객체를 생성
     */
    public static LocationResponse of(Double latitude, Double longitude) {
        return LocationResponse.builder()
                .latitude(latitude)
                .longitude(longitude)
                .build();
    }
}