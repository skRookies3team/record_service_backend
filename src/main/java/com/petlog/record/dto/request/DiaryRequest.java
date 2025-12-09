package com.petlog.record.dto.request;

import com.petlog.record.entity.Diary;
import com.petlog.record.entity.DiaryImage;
import com.petlog.record.entity.ImageSource;
import com.petlog.record.entity.Visibility;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class DiaryRequest {

    // [Request] 일기 생성
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "DiaryCreateRequest")
    public static class Create {

        @NotNull
        private Long userId;
        @NotNull
        private Long petId;

        private String content;
        private Visibility visibility;
        private Boolean isAiGen;
        private String weather;
        private String mood;

        // 이미지 리스트
        private List<Image> images;

        // DTO -> Diary Entity 변환 (DiaryImage 변환 시 userId를 전달)
        public Diary toEntity() {
            Diary diary = Diary.builder()
                    .userId(this.userId)
                    .petId(this.petId)
                    .content(this.content)
                    .visibility(this.visibility)
                    .isAiGen(this.isAiGen)
                    .weather(this.weather)
                    .mood(this.mood)
                    .build();

            if (this.images != null) {
                // [수정] Image::toEntity에 userId를 전달
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
    public static class Update {
        private String content;
        private Visibility visibility;
        private String weather;
        private String mood;
    }

    // [Inner DTO] 이미지 요청용
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Image {
        private String imageUrl;
        private Integer imgOrder;
        private Boolean mainImage;
        // [추가] 이미지 출처 (GALLERY, ARCHIVE)
        // 클라이언트가 "이 사진 갤러리에서 가져왔어요/보관함에서 골랐어요"라고 알려줘야 함
        private ImageSource source;

        // [수정] DTO -> DiaryImage Entity 변환 시 userId를 인자로 받음
        public DiaryImage toEntity(Long userId) {
            return DiaryImage.builder()
                    .userId(userId) // DiaryImage에 userId 저장
                    .imageUrl(this.imageUrl)
                    .imgOrder(this.imgOrder)
                    .mainImage(this.mainImage)
                    .source(this.source)
                    .build();
        }
    }
}