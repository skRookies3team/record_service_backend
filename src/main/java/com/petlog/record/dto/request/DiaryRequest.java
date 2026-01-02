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

    // [Request] 일기 생성
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

        // [추가] 위치 정보 (선택)
        @Schema(description = "위도", example = "37.5665")
        private Double latitude;

        @Schema(description = "경도", example = "126.9780")
        private Double longitude;

        // [수정] 주소명 필드 추가
        @Schema(description = "위치 주소 (직접 입력 시)", example = "서울 마포구")
        private String locationName;

        @Schema(description = "일기 날짜 (과거 일기 작성 시 필수)", example = "2023-10-25")
        private LocalDate date; // [추가] 날짜 필드

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

        // --- 추가된 필드: 미리보기 결과물을 다시 전달받기 위한 용도 ---

        @Schema(description = "미리보기 단계에서 발급받은 이미지 URL 목록")
        @JsonProperty("imageUrls")
        private List<String> imageUrls;

        @Schema(description = "미리보기 단계에서 발급받은 보관함(Archive) ID 목록")
        @JsonProperty("archiveIds")
        private List<Long> archiveIds;

        // DTO -> Diary Entity 변환
        public Diary toEntity() {
            Diary diary = Diary.builder()
                    .userId(this.userId)
                    .petId(this.petId)
                    .title(this.title) // 추가됨
                    .date(this.date)
                    //.photoArchiveId(this.photoArchiveId)
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

    // [Inner DTO] 이미지 요청용
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

        // ✅ [추가] MongoDB에 저장될 비정형 데이터 (EXIF, 기기정보, AI 태그 등)
        @Schema(description = "사진 메타데이터 (비정형)", example = "{\"camera\": \"iPhone 15\", \"location\": \"Seoul\"}")
        private Map<String, Object> metadata;

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