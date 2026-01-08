package com.petlog.record.dto.request;

import com.petlog.record.entity.DiaryStyle;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * [다이어리 스타일 설정 요청 DTO]
 * 일기장의 레이아웃, 폰트, 테마 등 시각적 요소를 정의하는 객체
 * 유저 기본 설정 -> 펫별 설정 -> 개별 일기 설정 순으로 오버라이딩(Overriding) 가능
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "다이어리 스타일 설정 요청 DTO")
public class DiaryStyleRequest {

    @Schema(description = "갤러리 레이아웃 타입 (grid, masonry, slider, classic)", example = "masonry")
    private String galleryType;

    @Schema(description = "텍스트 정렬 방식 (left, center, right)", example = "left")
    private String textAlignment;

    @Schema(description = "폰트 크기 (px 단위)", example = "16")
    private Integer fontSize;

    @Schema(description = "이미지 크기 옵션 (small, medium, large)", example = "medium")
    private String sizeOption;

    @Schema(description = "배경색 (HEX 코드)", example = "#FFFFFF")
    private String backgroundColor;

    @Schema(description = "적용할 프리셋 이름", example = "cozy_morning")
    private String preset;

    @Schema(description = "테마 스타일 (basic, romantic, modern 등)", example = "modern")
    private String themeStyle;

    // DiaryStyleRequest.java 안에 추가
    @Schema(description = "폰트 스타일", example = "Jua")
    private String fontFamily;

    /** [상속 계층] 특정 펫 전용 스타일일 경우의 펫 식별 ID (null이면 유저 공통 스타일) */
    @Schema(description = "스타일을 적용할 펫 ID (선택값: null일 경우 유저의 기본 스타일로 저장)", example = "1")
    private Long petId;

    /** [상속 계층] 특정 일기 한 건에만 적용할 경우의 다이어리 식별 ID */
    @Schema(description = "관련된 다이어리 ID (개별 다이어리 스타일 적용)", example = "100")
    private Long diaryId;

    /**
     * [엔티티 변환 메서드]
     * 전달받은 스타일 설정값을 기반으로 DiaryStyle 엔티티를 생성
     * 필드값이 null일 경우 시스템 기본값으로 대체하여 저장
     */
    public DiaryStyle toEntity(Long userId) {
        return DiaryStyle.builder()
                .userId(userId)
                .petId(this.petId)
                .diaryId(this.diaryId)
                .galleryType(this.galleryType)
                .textAlignment(this.textAlignment != null ? this.textAlignment : "left")
                .fontSize(this.fontSize != null ? this.fontSize : 16)
                .sizeOption(this.sizeOption != null ? this.sizeOption : "medium")
                .backgroundColor(this.backgroundColor != null ? this.backgroundColor : "#FFFFFF")
                .preset(this.preset != null ? this.preset : "default")
                .themeStyle(this.themeStyle != null ? this.themeStyle : "basic")
                // ✅ [NEW] 폰트 필드 추가 (기본값 설정)
                .fontFamily(this.fontFamily != null ? this.fontFamily : "Inter")
                .build();
    }
}
