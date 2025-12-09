package com.petlog.record.dto.response;

import com.petlog.record.entity.Recap;
import com.petlog.record.entity.RecapHighlight;
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
    public static class Detail {
        private Long recapId;
        private Long petId;
        private String title;
        private String summary;
        private LocalDate periodStart;
        private LocalDate periodEnd;
        private String mainImageUrl;
        private Integer momentCount;
        private String status;

        private Integer avgHeartRate;
        private Integer avgStepCount;
        private Double avgSleepTime;
        private Double avgWeight;

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
                    // 건강 데이터는 Service에서 주입하므로 여기선 skip
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
    public static class Simple {
        private Long recapId;
        private String title;
        private String mainImageUrl;
        private Integer momentCount;
        private String status;
        private LocalDate periodStart;
        private LocalDate periodEnd;
        private LocalDateTime createdAt;

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
                    .build();
        }
    }

    // [Inner] 하이라이트
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Highlight {
        private String title;
        private String content;

        public static Highlight fromEntity(RecapHighlight highlight) {
            return Highlight.builder()
                    .title(highlight.getTitle())
                    .content(highlight.getContent())
                    .build();
        }
    }
}