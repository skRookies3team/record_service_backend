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

public class RecapRequest {

    // [Request] AI 리캡 생성 요청 전용 DTO
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

    // [Request] 리캡 생성 및 저장용 (기존 필드 및 로직 유지)
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

        @Schema(description = "대표 이미지 URL", example = "https://bucket.s3.region.amazonaws.com/recap-cover.jpg")
        private String mainImageUrl;

        @Schema(description = "포함된 추억(일기) 개수", example = "15")
        private Integer momentCount;

        @Schema(description = "리캡 하이라이트 목록")
        private List<HighlightDto> highlights;

        // DTO -> Entity 변환 메서드
        public Recap toEntity() {
            Recap recap = Recap.builder()
                    .userId(this.userId)
                    .petId(this.petId)
                    .title(this.title)
                    .summary(this.summary)
                    .periodStart(this.periodStart)
                    .periodEnd(this.periodEnd)
                    .mainImageUrl(this.mainImageUrl)
                    .momentCount(this.momentCount)
                    .status(RecapStatus.GENERATED)
                    .build();

            if (this.highlights != null) {
                this.highlights.stream()
                        .map(HighlightDto::toEntity)
                        .forEach(recap::addHighlight);
            }

            return recap;
        }
    }

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

        public RecapHighlight toEntity() {
            return RecapHighlight.builder()
                    .title(this.title)
                    .content(this.content)
                    .build();
        }
    }
}