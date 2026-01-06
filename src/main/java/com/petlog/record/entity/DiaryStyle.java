package com.petlog.record.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * [다이어리 스타일 설정 엔티티]
 * 사용자가 설정한 일기장의 레이아웃, 폰트, 배경색 등 UI 렌더링 옵션을 저장
 * 유저 기본 스타일, 펫별 스타일, 개별 일기 스타일을 유연하게 처리하기 위한 구조
 */
@Entity
@Getter
@Setter // Service에서 Dirty Checking을 위해 필요
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "diary_styles")
public class DiaryStyle {

    // 스타일 ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 일기 ID
    @Column(name = "diary_id", nullable = false, updatable = false)
    private Long diaryId;

    // 사용자 ID (FK)
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 펫 ID (FK)
    @Column(name = "pet_id")
    private Long petId;

    // 갤러리 스타일
    @Column(name = "gallery_type", nullable = false, length = 50)
    @Builder.Default
    private String galleryType = "grid";

    // 텍스트 정렬
    @Column(name = "text_alignment", nullable = false, length = 20)
    @Builder.Default
    private String textAlignment = "left";

    // 글자 크기
    @Column(name = "font_size", nullable = false)
    @Builder.Default
    private Integer fontSize = 16;

    // 작게/크게 옵션
    @Column(name = "size_option", nullable = false, length = 20)
    @Builder.Default
    private String sizeOption = "medium";

    // 배경 색상
    @Column(name = "background_color", nullable = false, length = 7)
    @Builder.Default
    private String backgroundColor = "#FFFFFF";

    // 프리셋
    @Column(name = "preset", nullable = false, length = 50)
    @Builder.Default
    private String preset = "default";

    // 기본/로맨틱/모던 스타일
    @Column(name = "theme_style", nullable = false, length = 50)
    @Builder.Default
    private String themeStyle = "basic";

    // 작성일
    @CreationTimestamp
    private LocalDateTime createdAt;

    // 수정일
    @UpdateTimestamp
    private LocalDateTime updatedAt;

}