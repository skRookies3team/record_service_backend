package com.petlog.record.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * [실시간 위치 데이터 요청 DTO]
 * 모바일 클라이언트에서 백그라운드 트래킹을 통해 전달하는 위치 좌표 데이터
 * 수집된 데이터는 PostGIS를 통해 경로 분석 및 날씨 정보 매칭에 활용됨
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "실시간 위치 저장 요청 DTO")
public class LocationRequest {

    @NotNull
    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    /** 현재 위치의 위도 (WGS84 좌표계) */
    @NotNull
    @Schema(description = "위도", example = "37.5665")
    private Double latitude;

    /** 현재 위치의 경도 (WGS84 좌표계) */
    @NotNull
    @Schema(description = "경도", example = "126.9780")
    private Double longitude;
}