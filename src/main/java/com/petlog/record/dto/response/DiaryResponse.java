package com.petlog.record.dto.response;

import com.petlog.record.entity.Diary;
import com.petlog.record.entity.DiaryImage;
import com.petlog.record.entity.Visibility;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * [다이어리 상세 조회 응답 DTO]
 * 특정 일기의 모든 정보(RDB 기본 정보 + MongoDB 메타데이터 + UI 스타일)를 통합하여 반환하는 객체
 */
@Data
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "다이어리 상세 조회 응답 DTO")
public class DiaryResponse {

    @Schema(description = "다이어리 ID", example = "1")
    private Long diaryId;

    @Schema(description = "일기 제목", example = "햇살 좋은 날의 산책")
    private String title;

    @Schema(description = "작성자(사용자) ID", example = "1")
    private Long userId;

    @Schema(description = "관련 펫 ID", example = "1")
    private Long petId;

    @Schema(description = "일기 내용", example = "오늘 공원에서 산책하며 즐거운 시간을 보냈다.")
    private String content;

    @Schema(description = "일기 기록 날짜", example = "2023-10-25")
    private LocalDate date;

    @Schema(description = "위치 주소", example = "서울특별시 마포구...")
    private String locationName;

    @Schema(description = "위도", example = "37.5665")
    private Double latitude;

    @Schema(description = "경도", example = "126.9780")
    private Double longitude;

    @Schema(description = "공개 범위", example = "PUBLIC")
    private Visibility visibility;

    @Schema(description = "날씨", example = "맑음")
    private String weather;

    @Schema(description = "기분", example = "행복")
    private String mood;

    @Schema(description = "AI 생성 여부", example = "false")
    private Boolean isAiGen;

    @Schema(description = "작성일시", example = "2023-10-10T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2023-10-10T12:00:00")
    private LocalDateTime updatedAt;

    @Schema(description = "첨부 이미지 목록")
    private List<Image> images;

    @Schema(description = "다이어리 스타일 설정")
    private DiaryStyleResponse style;

    /**
     * [변환 메서드]
     * JPA 엔티티 객체를 기반으로 Response DTO를 생성
     */
    public static DiaryResponse fromEntity(Diary diary) {
        return DiaryResponse.builder()
                .diaryId(diary.getDiaryId())
                .title(diary.getTitle())
                .userId(diary.getUserId())
                .petId(diary.getPetId())
                .content(diary.getContent())
                .date(diary.getDate())
                .locationName(diary.getLocationName())
                .latitude(diary.getLatitude())
                .longitude(diary.getLongitude())
                .visibility(diary.getVisibility())
                .weather(diary.getWeather())
                .mood(diary.getMood())
                .isAiGen(diary.getIsAiGen())
                .createdAt(diary.getCreatedAt())
                .updatedAt(diary.getUpdatedAt())
                .images(diary.getImages().stream()
                        .map(img -> Image.fromEntity(img, null)) // 메타데이터 없이 호출
                        .collect(Collectors.toList()))
                .build();
    }

    /**
     * [이미지 상세 정보 DTO]
     * 개별 이미지의 DB 정보와 MongoDB의 비정형 메타데이터를 결합한 객체
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "다이어리 이미지 정보 DTO")
    public static class Image {

        @Schema(description = "이미지 ID", example = "10")
        private Long imageId;

        @Schema(description = "이미지 URL", example = "https://bucket.s3.region.amazonaws.com/image.jpg")
        private String imageUrl;

        @Schema(description = "이미지 순서", example = "1")
        private Integer imgOrder;

        @Schema(description = "대표 이미지 여부", example = "true")
        private Boolean mainImage;

        /** [MongoDB 연동] 이미지별 상세 비정형 데이터 (EXIF 정보 등) */
        @Schema(description = "이미지 상세 메타데이터(몽고DB)")
        private Map<String, Object> metadata;

        public static Image fromEntity(DiaryImage image, Map<String, Object> mongoMetadata) {
            return Image.builder()
                    .imageId(image.getImageId())
                    .imageUrl(image.getImageUrl())
                    .imgOrder(image.getImgOrder())
                    .mainImage(image.getMainImage())
                    .metadata(mongoMetadata)
                    .build();
        }
    }
}