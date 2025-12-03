package com.example.petlog.dto.request;

import com.example.petlog.entity.Diary;
import com.example.petlog.entity.DiaryImage;
import com.example.petlog.entity.PhotoArchive;
import com.example.petlog.entity.Visibility;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class DiaryRequest {

    // [Request] 일기 생성
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Create {

        @NotNull(message = "사용자 ID는 필수입니다.")
        private Long userId;

        @NotNull(message = "펫 ID는 필수입니다.")
        private Long petId;

        private String content;
        private Visibility visibility;
        private Boolean isAiGen;
        private String weather;
        private String mood;

        // 이미지 리스트
        private List<Image> images;

        // DTO -> Diary Entity 변환
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
                this.images.stream()
                        .map(Image::toEntity)
                        .forEach(diary::addImage);
            }

            return diary;
        }

        // [추가] DTO -> PhotoArchive Entity List 변환
        public List<PhotoArchive> toPhotoArchiveEntities() {
            if (this.images == null || this.images.isEmpty()) {
                return Collections.emptyList();
            }
            return this.images.stream()
                    .map(img -> PhotoArchive.builder()
                            .userId(this.userId) // 사용자 ID 사용
                            .imageUrl(img.getImageUrl())
                            .build())
                    .collect(Collectors.toList());
        }
    }

    // [Request] 일기 수정
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

        // DTO -> DiaryImage Entity 변환
        public DiaryImage toEntity() {
            return DiaryImage.builder()
                    .imageUrl(this.imageUrl)
                    .imgOrder(this.imgOrder)
                    .mainImage(this.mainImage)
                    .build();
        }
    }
}