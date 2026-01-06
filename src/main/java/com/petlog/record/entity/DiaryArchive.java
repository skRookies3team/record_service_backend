package com.petlog.record.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * [다이어리-보관함 매핑 엔티티]
 * 기록 서비스의 일기와 유저 서비스(MSA)의 사진 보관함(Archive) 간의 연결을 관리
 * 타 서비스의 리소스를 참조하므로 객체가 아닌 식별자(archiveId) 기반으로 관계 설정
 */
@Entity
@Table(name = "diary_archives")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DiaryArchive {

    // 매핑 고유 ID (PK)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 연결된 일기 엔티티 (FK)
    /** 연결된 일기 엔티티 (기록 서비스 내부 리소스) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diary_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)  // 👈 추가
    private Diary diary;

    // 연결된 보관함 사진 ID (외부 서비스 PK)
    /** * [MSA 외부 리소스 참조]
     * 유저 서비스(user-service)에 존재하는 사진 보관함의 고유 ID
     */
    @Column(name = "archive_id", nullable = false)
    private Long archiveId; // 필드명을 archiveId로 수정하여 의미를 명확히 함

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt; // 연결 생성 시각

    @UpdateTimestamp
    private LocalDateTime updatedAt; // 연결 수정 시각

    /**
     * [매핑 객체 생성]
     * 일기와 외부 보관함 ID를 받아 연관관계를 생성하는 정적 팩토리 메서드
     */
    public static DiaryArchive create(Diary diary, Long archiveId) {
        return DiaryArchive.builder()
                .diary(diary)
                .archiveId(archiveId)
                .build();
    }
}