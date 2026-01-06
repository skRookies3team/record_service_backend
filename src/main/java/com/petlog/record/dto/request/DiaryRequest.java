package com.petlog.record.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.petlog.record.entity.Diary;
import com.petlog.record.entity.DiaryImage;
import com.petlog.record.entity.ImageSource;
import com.petlog.record.entity.Visibility;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class DiaryRequest {

    /**
     * [다이어리 최종 생성 요청 DTO]
     * AI가 제안한 초안을 사용자가 수정한 후, 데이터베이스에 영구 저장하기 위해 전달하는 객체
     */
    @Data
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "일기 생성 요청 DTO")
    public static class Create {

        @NotNull(message = "작성자 ID는 필수입니다.")
        @Schema(description = "작성자(사용자) ID", example = "1")
        private Long userId;

        @NotNull(message = "펫 ID는 필수입니다.")
        @Schema(description = "관련된 펫 ID", example = "1")
        private Long petId;

        // ✅ 제목 필드 추가
        @Schema(description = "일기 제목", example = "초코와 함께한 즐거운 산책")
        private String title;

        @Schema(description = "관련된 사진 보관함 ID", example = "10")
        private Long photoArchiveId;

        // 위치 정보 (선택)
        @Schema(description = "위도", example = "37.5665")
        private Double latitude;

        @Schema(description = "경도", example = "126.9780")
        private Double longitude;

        // 주소명 필드 추가
        @Schema(description = "위치 주소 (직접 입력 시)", example = "서울 마포구")
        private String locationName;

        @Schema(description = "일기 날짜 (과거 일기 작성 시 필수)", example = "2023-10-25")
        private LocalDate date;

        @Schema(description = "일기 내용", example = "오늘 산책 너무 즐거웠어!")
        private String content;

        @Schema(description = "공개 범위 (PUBLIC, PRIVATE, FOLLOWER)", example = "PUBLIC")
        private Visibility visibility;

        @Schema(description = "AI 생성 여부", example = "false")
        private Boolean isAiGen;

        @Schema(description = "날씨", example = "맑음")
        private String weather;

        @Schema(description = "기분", example = "행복")
        private String mood;

        @Schema(description = "첨부 이미지 목록")
        private List<Image> images;

        /** [미리보기 연동] 이미지 업로드 후 발급받은 URL 목록 */

        @Schema(description = "미리보기 단계에서 발급받은 이미지 URL 목록")
        @JsonProperty("imageUrls")
        private List<String> imageUrls;

        /** [미리보기 연동] 이미지 서비스 보관함에 저장된 ID 목록 */
        @Schema(description = "미리보기 단계에서 발급받은 보관함(Archive) ID 목록")
        @JsonProperty("archiveIds")
        private List<Long> archiveIds;

        /**
         * [엔티티 변환 메서드]
         * DTO 데이터를 기반으로 Diary 도메인 엔티티를 생성
         */
        public Diary toEntity() {
            Diary diary = Diary.builder()
                    .userId(this.userId)
                    .petId(this.petId)
                    .title(this.title)
                    .date(this.date)
                    .content(this.content)
                    .visibility(this.visibility)
                    .isAiGen(this.isAiGen)
                    .weather(this.weather)
                    .mood(this.mood)
                    .build();

            if (this.images != null) {
                this.images.stream()
                        .map(img -> img.toEntity(this.userId))
                        .forEach(diary::addImage);
            }
            return diary;
        }
    }

    /**
     * [다이어리 정보 수정 요청 DTO]
     * 기존에 저장된 일기의 제목, 내용, 공개 범위 등을 변경할 때 사용
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "일기 수정 요청 DTO")
    public static class Update {

        // ✅ 수정 시 제목 필드 추가
        @Schema(description = "수정할 제목", example = "수정된 산책 일기")
        private String title;

        @Schema(description = "수정할 내용", example = "내용 수정됨")
        @JsonProperty("content") // 프론트에서 보내는 key값과 일치시켜야 함
        private String content;

        @Schema(description = "일기 날짜 (과거 일기 작성 시 필수)", example = "2023-10-25")
        private LocalDate date;

        @Schema(description = "공개 범위", example = "PRIVATE")
        private Visibility visibility;

        @Schema(description = "날씨", example = "흐림")
        private String weather;

        @Schema(description = "기분", example = "슬픔")
        private String mood;
    }

    /**
     * [이미지 정보 상세 DTO]
     * 일기에 포함되는 개별 이미지의 출처, 순서, 메타데이터 정보를 관리
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "이미지 정보 DTO")
    public static class Image {

        @Schema(description = "이미지 URL (S3)", example = "https://bucket.s3.ap-northeast-2.amazonaws.com/image.jpg")
        private String imageUrl;

        @Schema(description = "이미지 순서", example = "1")
        private Integer imgOrder;

        @Schema(description = "대표 이미지 여부", example = "true")
        private Boolean mainImage;

        // 기본값을 GALLERY로 설정하여 요청 시 생략 가능하도록 변경
        @Schema(description = "이미지 출처 (GALLERY, ARCHIVE)", example = "GALLERY", defaultValue = "GALLERY")
        @Builder.Default
        private ImageSource source = ImageSource.GALLERY;

        // ✅ archiveId 설명 추가
        @Schema(description = "이미지 출처가 ARCHIVE일 경우의 보관함 ID (source가 ARCHIVE일 때 필수)", example = "10")
        private Long archiveId;

        /** * [비정형 메타데이터]
         * MongoDB 등에 저장될 사진의 기술적 정보(EXIF, 위치 등)를 Key-Value 형태로 저장
         */
        @Schema(description = "사진 메타데이터 (비정형)", example = "{\"camera\": \"iPhone 15\", \"location\": \"Seoul\"}")
        private Map<String, Object> metadata;

        /**
         * [엔티티 변환 메서드]
         * 이미지 정보를 DiaryImage 엔티티로 매핑
         */
        public DiaryImage toEntity(Long userId) {
            return DiaryImage.builder()
                    .userId(userId)
                    .imageUrl(this.imageUrl)
                    .imgOrder(this.imgOrder)
                    .mainImage(this.mainImage)
                    .source(this.source)
                    .build();
        }
    }
}