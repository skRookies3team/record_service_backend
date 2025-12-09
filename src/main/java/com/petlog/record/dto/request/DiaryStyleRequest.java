package com.petlog.record.dto.request;

import com.petlog.record.entity.DiaryStyle; // Entity 경로 수정 필요
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiaryStyleRequest {
    private String galleryType;      // 'grid', 'masonry', 'slider', 'classic'
    private String textAlignment;    // 'left', 'center', 'right'
    private Integer fontSize;        // 16, 18, 20 등
    private String sizeOption;       // 'small', 'medium', 'large'
    private String backgroundColor;  // '#FFFFFF' 형식
    private String preset;           // 프리셋 이름
    private String themeStyle;       // 'basic', 'romantic', 'modern'
    private Long petId;              // 스타일을 적용할 펫 ID (선택적)

    // [추가] DTO -> Entity 변환
    public DiaryStyle toEntity(Long userId) {
        return DiaryStyle.builder()
                .userId(userId)
                .petId(this.petId)
                .galleryType(this.galleryType)
                .textAlignment(this.textAlignment)
                .fontSize(this.fontSize)
                .sizeOption(this.sizeOption)
                .backgroundColor(this.backgroundColor)
                .preset(this.preset)
                .themeStyle(this.themeStyle)
                .build();
    }
}