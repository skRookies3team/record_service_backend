package com.petlog.record.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "diary_archives")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DiaryArchive {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diary_id", nullable = false)
    private Diary diary;

    @JoinColumn(name = "archive_id", nullable = false)
    private Long archive;

    // Helper method for creation
    public static DiaryArchive create(Diary diary, Long archive) {
        return DiaryArchive.builder()
                .diary(diary)
                .archive(archive)
                .build();
    }
}