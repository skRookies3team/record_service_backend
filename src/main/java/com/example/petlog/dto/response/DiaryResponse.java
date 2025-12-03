package com.example.petlog.dto.response;

import com.example.petlog.entity.Diary;
import com.example.petlog.entity.DiaryImage;
import com.example.petlog.entity.Visibility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiaryResponse {

    private Long diaryId;
    private Long userId;
    private Long petId;
    private String content;
    private Visibility visibility;
    private String weather;
    private String mood;
    private Boolean isAiGen; // AI 여부 확인용
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<Image> images;

    // [핵심] Entity -> DTO 변환 로직 (Service에서 호출)
    public static DiaryResponse fromEntity(Diary diary) {
        return DiaryResponse.builder()
                .diaryId(diary.getDiaryId())
                .userId(diary.getUserId())
                .petId(diary.getPetId())
                .content(diary.getContent())
                .visibility(diary.getVisibility())
                .weather(diary.getWeather())
                .mood(diary.getMood())
                .isAiGen(diary.getIsAiGen())
                .createdAt(diary.getCreatedAt())
                .updatedAt(diary.getUpdatedAt())
                .images(diary.getImages().stream()
                        .map(Image::fromEntity)
                        .collect(Collectors.toList()))
                .build();
    }

    // [Inner DTO] 이미지 응답용
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Image {

        private Long imageId;
        private String imageUrl;
        private Integer imgOrder;
        private Boolean mainImage;

        // Entity -> DTO 변환
        public static Image fromEntity(DiaryImage image) {
            return Image.builder()
                    .imageId(image.getImageId())
                    .imageUrl(image.getImageUrl())
                    .imgOrder(image.getImgOrder())
                    .mainImage(image.getMainImage())
                    .build();
        }
    }
}