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

/**
 * [리캡 정보 조회 응답 DTO 그룹]
 * 데이터베이스에 저장된 리캡 정보를 사용자에게 보여주기 위한 객체들의 모음
 */
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

        /** 리캡을 구성하는 대표 이미지들 (최대 8장으로 제한하여 UX 최적화) */
        @Schema(description = "리캡에 포함된 대표 이미지 리스트 (최대 8장)")
        private List<String> imageUrls;

        @Schema(description = "분석에 포함된 추억(일기) 개수", example = "15")
        private Integer momentCount;

        /** 리캡 생성 상태 (WAITING: 대기 중, GENERATED: 완료) */
        @Schema(description = "생성 상태 (GENERATED, WAITING)", example = "GENERATED")
        private String status;

        @Schema(description = "생성 일시")
        private LocalDateTime createdAt;

        @Schema(description = "수정 일시")
        private LocalDateTime updatedAt;

        @Schema(description = "리캡 하이라이트 목록 (AI 분석 결과)")
        private List<Highlight> highlights;

        /** [변환 메서드] Recap 엔티티를 상세 응답 DTO로 변환 */
        public static Detail fromEntity(Recap recap) {
            return Detail.builder()
                    .recapId(recap.getRecapId())
                    .petId(recap.getPetId())
                    .title(recap.getTitle())
                    .summary(recap.getSummary())
                    .periodStart(recap.getPeriodStart())
                    .periodEnd(recap.getPeriodEnd())
                    .imageUrls(recap.getImageUrls())
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

    /**
     * [리캡 목록 조회용 요약 DTO]
     * 사용자의 리캡 히스토리 목록 화면에서 성능 최적화를 위해 필요한 최소 정보만 포함
     */
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

        /** 목록 썸네일용 대표 이미지 (이미지 리스트 중 첫 번째 사진 활용) */
        @Schema(description = "목록 표시용 대표 이미지 (이미지 리스트 중 첫 번째)")
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

        /** [변환 메서드] Recap 엔티티를 목록용 요약 DTO로 변환 */
        public static Simple fromEntity(Recap recap) {
            return Simple.builder()
                    .recapId(recap.getRecapId())
                    .title(recap.getTitle())
                    // 목록에서는 첫 번째 이미지만 대표로 보여줌
                    .mainImageUrl(recap.getImageUrls().isEmpty() ? null : recap.getImageUrls().get(0))
                    .momentCount(recap.getMomentCount())
                    .status(recap.getStatus().name())
                    .periodStart(recap.getPeriodStart())
                    .periodEnd(recap.getPeriodEnd())
                    .createdAt(recap.getCreatedAt())
                    .updatedAt(recap.getUpdatedAt())
                    .build();
        }
    }

    /**
     * [리캡 하이라이트 정보 객체]
     * 한 달 중 가장 의미 있는 순간에 대한 AI 분석 제목과 내용
     */
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

        /** [변환 메서드] 하이라이트 엔티티를 응답 객체로 변환 */
        public static Highlight fromEntity(RecapHighlight highlight) {
            return Highlight.builder()
                    .title(highlight.getTitle())
                    .content(highlight.getContent())
                    .build();
        }
    }
}