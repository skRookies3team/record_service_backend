package com.petlog.record.dto.response;

import com.petlog.record.entity.DiaryStyle;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * [다이어리 스타일 설정 응답 DTO]
 * 일기장의 레이아웃 및 디자인 프리셋 등 커스터마이징된 설정값을 반환하는 객체
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "다이어리 스타일 설정 응답 DTO")
public class DiaryStyleResponse {

    @Schema(description = "스타일 설정 ID", example = "1")
    private Long id;

    @Schema(description = "관련된 다이어리 ID", example = "100")
    private Long diaryId;

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "적용된 펫 ID (null인 경우 사용자의 기본 스타일)", example = "1")
    private Long petId;

    @Schema(description = "갤러리 레이아웃 타입 (grid, masonry, slider, classic)", example = "masonry")
    private String galleryType;

    @Schema(description = "텍스트 정렬 방식 (left, center, right)", example = "left")
    private String textAlignment;

    @Schema(description = "폰트 크기 (px)", example = "16")
    private Integer fontSize;

    @Schema(description = "이미지 크기 옵션 (small, medium, large)", example = "medium")
    private String sizeOption;

    @Schema(description = "배경색 (HEX 코드)", example = "#FFFFFF")
    private String backgroundColor;

    @Schema(description = "적용된 프리셋 이름", example = "cozy_morning")
    private String preset;

    @Schema(description = "테마 스타일 (basic, romantic, modern 등)", example = "modern")
    private String themeStyle;

    // ✅ [NEW] 폰트 필드 추가
    @Schema(description = "폰트 스타일(Jua, Noto Sans KR 등)", example = "Jua")
    private String fontFamily;

    @Schema(description = "생성일시")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시")
    private LocalDateTime updatedAt;

    /**
     * [변환 메서드]
     * DiaryStyle 엔티티를 UI 렌더링에 적합한 DTO로 변환
     */
    public static DiaryStyleResponse fromEntity(DiaryStyle style) {
        return DiaryStyleResponse.builder()
                .id(style.getId())
                .diaryId(style.getDiaryId())
                .userId(style.getUserId())
                .petId(style.getPetId())
                .galleryType(style.getGalleryType())
                .textAlignment(style.getTextAlignment())
                .fontSize(style.getFontSize())
                .sizeOption(style.getSizeOption())
                .backgroundColor(style.getBackgroundColor())
                .preset(style.getPreset())
                .themeStyle(style.getThemeStyle())
                .fontFamily(style.getFontFamily()) // ✅ [NEW] 여기서 Entity의 값을 DTO로 복사
                .createdAt(style.getCreatedAt())
                .updatedAt(style.getUpdatedAt())
                .build();
    }
}