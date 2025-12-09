package com.petlog.record.dto.response;

import com.petlog.record.entity.DiaryStyle; // Entity 경로 수정 필요
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiaryStyleResponse {
    private Long id;
    private Long userId;
    private Long petId;
    private String galleryType;
    private String textAlignment;
    private Integer fontSize;
    private String sizeOption;
    private String backgroundColor;
    private String preset;
    private String themeStyle;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // [추가] Entity -> DTO 변환
    public static DiaryStyleResponse fromEntity(DiaryStyle style) {
        return DiaryStyleResponse.builder()
                .id(style.getId())
                .userId(style.getUserId())
                .petId(style.getPetId())
                .galleryType(style.getGalleryType())
                .textAlignment(style.getTextAlignment())
                .fontSize(style.getFontSize())
                .sizeOption(style.getSizeOption())
                .backgroundColor(style.getBackgroundColor())
                .preset(style.getPreset())
                .themeStyle(style.getThemeStyle())
                .createdAt(style.getCreatedAt())
                .updatedAt(style.getUpdatedAt())
                .build();
    }
}