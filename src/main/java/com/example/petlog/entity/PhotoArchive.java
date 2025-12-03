package com.example.petlog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "PHOTO_ARCHIVES")
public class PhotoArchive {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long archiveId;

    // 일기 ID가 아닌 사용자 ID를 가집니다. (일기가 지워져도 사용자의 사진으로 남음)
    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String imageUrl;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}