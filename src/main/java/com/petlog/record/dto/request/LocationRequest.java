package com.petlog.record.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "실시간 위치 저장 요청 DTO")
public class LocationRequest {

    @NotNull
    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @NotNull
    @Schema(description = "위도", example = "37.5665")
    private Double latitude;

    @NotNull
    @Schema(description = "경도", example = "126.9780")
    private Double longitude;
}