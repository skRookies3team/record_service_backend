package com.petlog.record.dto.response;

import com.petlog.record.entity.Recap;
import com.petlog.record.entity.RecapHighlight;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class RecapResponse {

    // [Detail] 상세 조회용
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "리캡 상세 조회 응답 DTO")
    public static class Detail {
        @Schema(description = "리캡 ID", example = "100")
        private Long recapId;

        @Schema(description = "펫 ID", example = "1")
        private Long petId;

        @Schema(description = "리캡 제목 (AI 생성)", example = "2024년 3월의 소중한 기록")
        private String title;

        @Schema(description = "리캡 요약 문구 (AI 생성)", example = "봄바람을 맞으며 산책하는 것을 가장 좋아했어요.")
        private String summary;

        @Schema(description = "집계 기간 시작일", example = "2024-03-01")
        private LocalDate periodStart;

        @Schema(description = "집계 기간 종료일", example = "2024-03-31")
        private LocalDate periodEnd;

        @Schema(description = "대표 이미지 URL (기존 일기 중 선정되거나 기본 이미지)", example = "https://bucket.s3.region.amazonaws.com/cover.jpg")
        private String mainImageUrl;

        @Schema(description = "분석에 포함된 추억(일기) 개수", example = "15")
        private Integer momentCount;

        @Schema(description = "생성 상태 (GENERATED, WAITING)", example = "GENERATED")
        private String status;

        @Schema(description = "생성 일시")
        private LocalDateTime createdAt;

        @Schema(description = "수정 일시")
        private LocalDateTime updatedAt;

        @Schema(description = "리캡 하이라이트 목록 (AI 분석 결과)")
        private List<Highlight> highlights;

        // Entity -> DTO 변환 (Factory Method)
        public static Detail fromEntity(Recap recap) {
            return Detail.builder()
                    .recapId(recap.getRecapId())
                    .petId(recap.getPetId())
                    .title(recap.getTitle())
                    .summary(recap.getSummary())
                    .periodStart(recap.getPeriodStart())
                    .periodEnd(recap.getPeriodEnd())
                    .mainImageUrl(recap.getMainImageUrl())
                    .momentCount(recap.getMomentCount())
                    .status(recap.getStatus().name())
                    .createdAt(recap.getCreatedAt())
                    .updatedAt(recap.getUpdatedAt())
                    .highlights(recap.getHighlights().stream()
                            .map(Highlight::fromEntity)
                            .collect(Collectors.toList()))
                    .build();
        }
    }

    // [Simple] 리스트 조회용
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "리캡 목록 조회용 요약 DTO")
    public static class Simple {
        @Schema(description = "리캡 ID", example = "100")
        private Long recapId;

        @Schema(description = "리캡 제목", example = "2024년 3월의 기록")
        private String title;

        @Schema(description = "대표 이미지 URL", example = "https://bucket.s3.region.amazonaws.com/thumbnail.jpg")
        private String mainImageUrl;

        @Schema(description = "추억 개수", example = "15")
        private Integer momentCount;

        @Schema(description = "상태", example = "GENERATED")
        private String status;

        @Schema(description = "시작일", example = "2024-03-01")
        private LocalDate periodStart;

        @Schema(description = "종료일", example = "2024-03-31")
        private LocalDate periodEnd;

        @Schema(description = "생성일시")
        private LocalDateTime createdAt;

        @Schema(description = "수정일시")
        private LocalDateTime updatedAt;

        public static Simple fromEntity(Recap recap) {
            return Simple.builder()
                    .recapId(recap.getRecapId())
                    .title(recap.getTitle())
                    .mainImageUrl(recap.getMainImageUrl())
                    .momentCount(recap.getMomentCount())
                    .status(recap.getStatus().name())
                    .periodStart(recap.getPeriodStart())
                    .periodEnd(recap.getPeriodEnd())
                    .createdAt(recap.getCreatedAt())
                    .updatedAt(recap.getUpdatedAt())
                    .build();
        }
    }

    // [Inner] 하이라이트
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "리캡 하이라이트 정보")
    public static class Highlight {
        @Schema(description = "하이라이트 제목", example = "한강 공원 나들이")
        private String title;

        @Schema(description = "하이라이트 내용", example = "처음으로 친구를 만난 날이에요!")
        private String content;

        public static Highlight fromEntity(RecapHighlight highlight) {
            return Highlight.builder()
                    .title(highlight.getTitle())
                    .content(highlight.getContent())
                    .build();
        }
    }
}