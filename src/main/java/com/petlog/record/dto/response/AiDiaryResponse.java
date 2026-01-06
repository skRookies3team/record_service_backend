package com.petlog.record.dto.response;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * [AI 일기 미리보기 응답 DTO]
 * 사진 분석 및 위치 정보를 기반으로 AI가 생성한 일기 초안 데이터를 담는 객체
 * 최종 저장 전 사용자에게 보여줄 '미리보기' 화면의 데이터 소스로 사용됨
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class AiDiaryResponse {

    // ✅ AI가 생성한 제목 필드 추가
    @JsonProperty("title")
    private String title;
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
    // ✅ 기록될 날짜 정보 추가
    @JsonProperty("date")
    private LocalDate date;
    // NOTE: diaryId는 저장되지 않았으므로 포함하지 않습니다.

    /** [MongoDB 연동] AI가 사진별로 추출한 비정형 메타데이터(사물 인식, 태그 등) 목록 */
    @JsonProperty("imagesMetadata")
    private List<Map<String, Object>> imagesMetadata;

    /** 이미지 서비스에서 발급받은 S3 접근 URL 리스트 */
    @JsonProperty("imageUrls")
    private List<String> imageUrls;

    /** 이미지 서비스 보관함(Archive)에 저장된 고유 ID 리스트 */
    @JsonProperty("archiveIds")
    private List<Long> archiveIds;
}