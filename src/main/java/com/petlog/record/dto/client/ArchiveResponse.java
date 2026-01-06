package com.petlog.record.dto.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * [이미지 서비스 응답 DTO]
 * 유저 서비스의 Archive API 호출 결과(S3 업로드 정보 등)를 담는 객체
 */
public class ArchiveResponse {

    /**
     * [개별 보관함 생성 정보]
     * 업로드된 개별 이미지의 식별자 및 접근 가능한 S3 URL 정보를 포함
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateArchiveDto {
        private Long archiveId;
        private String url;  // S3에 저장된 이미지 전체 경로 URL
        private LocalDateTime uploadTime;
    }

    /**
     * [보관함 생성 결과 리스트]
     * 다중 이미지 업로드 시 생성된 Archive 정보를 리스트 형태로 래핑
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateArchiveDtoList {
        private List<CreateArchiveDto> archives;  // 생성된 이미지 정보 목록
    }
}