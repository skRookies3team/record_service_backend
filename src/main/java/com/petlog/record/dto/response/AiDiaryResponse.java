package com.petlog.record.dto.response;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class AiDiaryResponse {

    // AI가 생성한 일기 본문
    @JsonProperty("content")
    private String content;
    // AI가 분석한 날씨
    @JsonProperty("weather")
    private String weather;
    // AI가 분석한 기분 (예: "행복", "나른함")
    @JsonProperty("mood")
    private String mood;
    // AI가 추론하거나 요청받은 위치 명
    @JsonProperty("locationName")
    private String locationName;
    @JsonProperty("latitude")
    private Double latitude;
    @JsonProperty("longitude")
    private Double longitude;
    // NOTE: diaryId는 저장되지 않았으므로 포함하지 않습니다.

    // --- 추가된 필드 ---
    @JsonProperty("imageUrls")
    private List<String> imageUrls; // S3에 업로드된 이미지 경로들
    @JsonProperty("archiveIds")
    private List<Long> archiveIds;   // 생성된 보관함 ID들
}