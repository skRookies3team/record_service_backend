package com.example.petlog.dto.request;

import com.example.petlog.entity.Recap;
import com.example.petlog.entity.RecapHighlight;
import com.example.petlog.entity.RecapStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

public class RecapRequest {

    // [Request] 리캡 임시 생성
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "RecapCreateRequest")
    public static class Create {
        @NotNull
        private Long petId;
        @NotNull
        private Long userId;

        private String title;
        private String summary;
        private LocalDate periodStart;
        private LocalDate periodEnd;
        private String mainImageUrl;
        private Integer momentCount;

        // 하이라이트 목록
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
                    .status(RecapStatus.GENERATED) // 기본 상태 설정
                    .build();

            // 하이라이트 리스트가 있다면 Entity로 변환하여 추가
            if (this.highlights != null) {
                this.highlights.stream()
                        .map(HighlightDto::toEntity)
                        .forEach(recap::addHighlight); // Recap의 연관관계 편의 메서드 사용
            }

            return recap;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HighlightDto {
        private String title;
        private String content;

        // DTO -> Entity 변환
        public RecapHighlight toEntity() {
            return RecapHighlight.builder()
                    .title(this.title)
                    .content(this.content)
                    .build();
        }
    }
}