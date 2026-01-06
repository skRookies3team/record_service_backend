package com.petlog.record.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * [AI 월간 리캡 엔티티]
 * 특정 기간(월간)의 반려동물 활동 데이터를 집계하여 AI가 생성한 요약 리포트
 */
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "RECAPS")
public class Recap {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long recapId;

    @Column(nullable = false)
    private Long petId;

    @Column(nullable = false)
    private Long userId;

    /** 리캡 명칭 (예: "2024년 3월의 추억") */
    @Column(nullable = false)
    private String title;

    /** AI가 일기 맥락을 분석하여 작성한 총평 요약 */
    @Column(columnDefinition = "TEXT")
    private String summary;

    /** 리캡 분석 대상 시작일 */
    private LocalDate periodStart;

    /** 리캡 분석 대상 종료일 */
    private LocalDate periodEnd;

    /** 리캡에 포함된 대표 사진 URL 목록 (최대 8장) */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "RECAP_IMAGES", joinColumns = @JoinColumn(name = "recap_id"))
    @Column(name = "image_url")
    @Builder.Default
    private List<String> imageUrls = new ArrayList<>();

    /** 해당 기간 내 작성된 총 일기(순간) 개수 */
    private Integer momentCount;

    /** 리캡 생성 상태 (WAITING: 스케줄링 대기, GENERATED: 분석 완료) */
    @Enumerated(EnumType.STRING)
    private RecapStatus status;

    /** AI가 선정한 월간 주요 하이라이트 장면 (1:N) */
    @Builder.Default
    @OneToMany(mappedBy = "recap", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecapHighlight> highlights = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /** [비즈니스 로직] 하이라이트 추가 및 양방향 연관관계 설정 */
    public void addHighlight(RecapHighlight highlight) {
        this.highlights.add(highlight);
        highlight.setRecap(this);
    }

    /** 이미지 목록 갱신 편의 메서드 */
    public void updateImageUrls(List<String> urls) {
        this.imageUrls = urls;
    }
}

