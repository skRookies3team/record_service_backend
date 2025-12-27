package com.petlog.record.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

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
    // Diary 엔티티와 연관관계를 맺어 어떤 일기에 속한 사진인지 식별합니다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diary_id", nullable = false)
    private Diary diary;

    // 연결된 보관함 사진 ID (외부 서비스 PK)
    // user-service의 Archive 엔티티는 다른 DB에 있으므로, 객체 대신 ID(Long)값만 저장합니다.
    @Column(name = "archive_id", nullable = false)
    private Long archiveId; // 필드명을 archiveId로 수정하여 의미를 명확히 함

    // --- [추가 필드] ---
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt; // 연결 생성 시각

    @UpdateTimestamp
    private LocalDateTime updatedAt; // 연결 수정 시각

    // === [생성 편의 메서드] ===
    /**
     * 일기와 보관함 ID를 받아 매핑 객체를 생성합니다.
     */
    public static DiaryArchive create(Diary diary, Long archiveId) {
        return DiaryArchive.builder()
                .diary(diary)
                .archiveId(archiveId)
                .build();
    }
}