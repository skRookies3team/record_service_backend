package com.petlog.record.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "위치 정보 응답 DTO")
public class LocationResponse {

    @Schema(description = "위도", example = "37.5665")
    private Double latitude;

    @Schema(description = "경도", example = "126.9780")
    private Double longitude;

    // 정적 팩토리 메서드 (편의용)
    public static LocationResponse of(Double latitude, Double longitude) {
        return LocationResponse.builder()
                .latitude(latitude)
                .longitude(longitude)
                .build();
    }
}