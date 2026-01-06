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
 * [다이어리 핵심 엔티티 - PostgreSQL]
 * 반려동물의 일상 기록을 저장하는 도메인의 중심 엔티티
 * 위치(PostGIS 기반), 날씨, 기분, AI 생성 여부 등 풍부한 메타데이터를 관리
 */
@Entity
@Getter
@Builder // 클래스 레벨로 이동: 모든 필드를 대상으로 빌더 생성 가능
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor // 빌더 패턴 사용 시 전체 생성자가 필요
@Table(name = "DIARIES")
public class Diary {

    // 일기 ID
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long diaryId;

    // ✅ 제목 필드
    @Column(length = 200)
    private String title;

    // 작성자 ID (FK) - 필수값
    @Column(nullable = false)
    private Long userId;

    // 펫 ID (FK) - 필수값
    @Column(nullable = false)
    private Long petId;

    // 일기 내용 (TEXT 타입, 사진만 올릴 수 있으므로 null 허용)
    @Column(columnDefinition = "TEXT")
    private String content;

    // 공개 범위 (PUBLIC, FOLLOWER, PRIVATE) - 필수값
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Visibility visibility;

    // AI 작성 여부 - 필수값
    @Column(nullable = false)
    private Boolean isAiGen;

    // --- 위치 정보 및 날짜 필드 ---
    private String locationName; // 주소 (예: 서울특별시 마포구)

    private Double latitude;     // 위도

    private Double longitude;    // 경도

    /** 일기 기록 날짜 (사용자가 과거 날짜를 선택할 수 있으므로 createdAt과 별도 관리) */
    private LocalDate date;      // 일기 날짜 (실제 기록된 날짜)
    // -------------------------------------

    // 날씨 (선택 입력)
    private String weather;

    // 기분 (선택 입력)
    private String mood;

    // 작성일
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // 수정일
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // 일기 이미지 목록
    // @Builder.Default: 빌더로 생성할 때도 이 필드가 null이 아닌 빈 리스트(new ArrayList)로 초기화됨
    /** 일기에 포함된 이미지 리스트 (CASCADE를 통해 생명주기 함께 관리) */
    @Builder.Default
    @OneToMany(mappedBy = "diary", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DiaryImage> images = new ArrayList<>();

    // === [비즈니스 로직] ===

    /**
     * [일기 정보 수정]
     * 제목, 내용, 날짜 등 일기의 핵심 정보를 갱신하는 비즈니스 메서드
     */
    public void update(String title, String content, LocalDate date, Visibility visibility, String weather, String mood) {
        this.title = title;
        this.content = content;
        this.date = date;
        this.visibility = visibility;
        this.weather = weather;
        this.mood = mood;
    }

    /**
     * [연관관계 편의 메서드]
     * 다이어리에 새로운 이미지를 추가하고 양방향 연관관계를 설정
     */
    public void addImage(DiaryImage image) {
        this.images.add(image);
        image.setDiary(this);
    }
}