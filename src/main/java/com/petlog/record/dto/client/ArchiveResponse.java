package com.petlog.record.dto.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 유저 서비스(8080)의 Archive API 응답을 받아오기 위한 DTO
 */
public class ArchiveResponse {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateArchiveDto {
        private Long archiveId;
        private String url;
        private LocalDateTime uploadTime;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateArchiveDtoList {
        private List<CreateArchiveDto> archives;
    }
}