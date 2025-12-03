package com.example.petlog.dto.response;

import com.example.petlog.entity.PhotoArchive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhotoArchiveResponse {

    private Long archiveId;
    private Long userId;
    private String imageUrl;
    private LocalDateTime createdAt;

    public static PhotoArchiveResponse fromEntity(PhotoArchive archive) {
        return PhotoArchiveResponse.builder()
                .archiveId(archive.getArchiveId())
                .userId(archive.getUserId())
                .imageUrl(archive.getImageUrl())
                .createdAt(archive.getCreatedAt())
                .build();
    }
}