package com.petlog.record.dto.request;

import com.petlog.record.entity.Recap;
import com.petlog.record.entity.RecapHighlight;
import com.petlog.record.entity.RecapStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * [AI 월간 리캡 요청 DTO]
 * 특정 기간의 다이어리 데이터를 집계하여 AI 요약 콘텐츠를 생성하고 저장하기 위한 객체군
 */
public class RecapRequest {

    /**
     * [AI 리캡 생성 요청 DTO]
     * 특정 펫 한 마리에 대해 지정된 기간의 일기 데이터를 분석하여 리캡 생성을 요청할 때 사용
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "RecapGenerateRequest", description = "AI 리캡 생성 요청 DTO")
    public static class Generate {

        @NotNull(message = "펫 ID는 필수입니다.")
        @Schema(description = "리캡을 생성할 펫 ID", example = "1")
        private Long petId;

        @NotNull(message = "사용자 ID는 필수입니다.")
        @Schema(description = "사용자 ID", example = "1")
        private Long userId;

        @NotNull(message = "집계 기간 시작일은 필수입니다.")
        @Schema(description = "집계 기간 시작일", example = "2024-03-01")
        private LocalDate periodStart;

        @NotNull(message = "집계 기간 종료일은 필수입니다.")
        @Schema(description = "집계 기간 종료일", example = "2024-03-31")
        private LocalDate periodEnd;

        @Schema(description = "펫 이름 (AI 프롬프트 최적화용)", example = "초코")
        private String petName;
    }

    /**
     * [모든 펫 리캡 일괄 생성 요청 DTO]
     * 사용자가 보유한 모든 반려동물에 대해 특정 기간의 리캡을 한 번에 생성 요청할 때 사용
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "RecapGenerateAllRequest", description = "모든 펫 리캡 일괄 생성 요청 DTO")
    public static class GenerateAll {

        @NotNull(message = "사용자 ID는 필수입니다.")
        @Schema(description = "사용자 ID", example = "1")
        private Long userId;

        @NotNull(message = "집계 기간 시작일은 필수입니다.")
        @Schema(description = "집계 기간 시작일", example = "2024-03-01")
        private LocalDate periodStart;

        @NotNull(message = "집계 기간 종료일은 필수입니다.")
        @Schema(description = "집계 기간 종료일", example = "2024-03-31")
        private LocalDate periodEnd;
    }

    /**
     * [리캡 생성 및 저장 요청 DTO]
     * AI 분석이 완료된 최종 결과물 또는 스케줄러에 의한 예약(WAITING) 데이터를 DB에 저장할 때 사용
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "RecapCreateRequest", description = "리캡 생성 및 저장 요청 DTO")
    public static class Create {

        @NotNull(message = "펫 ID는 필수입니다.")
        @Schema(description = "리캡을 생성할 펫 ID", example = "1")
        private Long petId;

        @NotNull(message = "사용자 ID는 필수입니다.")
        @Schema(description = "사용자 ID", example = "1")
        private Long userId;

        @Schema(description = "리캡 제목", example = "2024년 3월의 추억")
        private String title;

        @Schema(description = "리캡 요약 문구", example = "산책을 많이 다녀서 즐거웠던 한 달!")
        private String summary;

        @Schema(description = "집계 기간 시작일", example = "2024-03-01")
        private LocalDate periodStart;

        @Schema(description = "집계 기간 종료일", example = "2024-03-31")
        private LocalDate periodEnd;

        @Schema(description = "대표 이미지 URL 리스트", example = "[\"https://image1.jpg\", \"https://image2.jpg\"]")
        private List<String> imageUrls;

        @Schema(description = "포함된 추억(일기) 개수", example = "15")
        private Integer momentCount;

        @Schema(description = "리캡 하이라이트 목록")
        private List<HighlightDto> highlights;

        @Schema(description = "리캡 상태", example = "WAITING")
        private String status; // 추가

        /**
         * [엔티티 변환 메서드]
         * DTO 데이터를 Recap 도메인 엔티티로 매핑하며, 상태값이 없을 경우 기본값(GENERATED) 적용
         */
        public Recap toEntity() {
            RecapStatus recapStatus = (this.status != null)
                    ? RecapStatus.valueOf(this.status)
                    : RecapStatus.GENERATED;

            Recap recap = Recap.builder()
                    .userId(this.userId)
                    .petId(this.petId)
                    .title(this.title)
                    .summary(this.summary)
                    .periodStart(this.periodStart)
                    .periodEnd(this.periodEnd)
                    .imageUrls(this.imageUrls)
                    .momentCount(this.momentCount)
                    .status(recapStatus) // 동적으로 설정
                    .build();


            if (this.highlights != null) {
                this.highlights.stream()
                        .map(HighlightDto::toEntity)
                        .forEach(recap::addHighlight);
            }

            return recap;
        }
    }

    /**
     * [리캡 하이라이트 정보 DTO]
     * 한 달 중 가장 의미 있었던 특정 순간의 제목과 내용을 담는 객체
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "리캡 하이라이트 정보 DTO")
    public static class HighlightDto {

        @Schema(description = "하이라이트 제목", example = "한강 공원 나들이")
        private String title;

        @Schema(description = "하이라이트 내용", example = "처음으로 강아지 친구를 만났어요.")
        private String content;

        /** 하이라이트 정보를 RecapHighlight 엔티티로 변환 */
        public RecapHighlight toEntity() {
            return RecapHighlight.builder()
                    .title(this.title)
                    .content(this.content)
                    .build();
        }
    }
}