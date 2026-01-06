package com.petlog.record.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * [AI 리캡 분석 결과 응답 DTO]
 * AI 서비스(LLM)가 한 달간의 데이터를 분석하여 생성한 원천 데이터를 담는 내부용 객체
 * 이 데이터를 바탕으로 사용자에게 보여줄 최종 리캡 엔티티(Recap)를 구성함
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI 분석 결과 데이터 (내부용)")
public class RecapAiResponse {

    @Schema(description = "AI가 생성한 리캡 제목", example = "초코와 함께한 따스한 3월")
    private String title;

    @Schema(description = "AI가 분석한 한 달 요약 총평", example = "이번 달 초코는 새로운 친구들을 많이 만나며 사회성이 부쩍 좋아진 한 달을 보냈어요.")
    private String summary;

    @Schema(description = "AI가 선정한 이번 달의 주요 하이라이트 목록")
    private List<HighlightInfo> highlights;

    /**
     * [AI 분석 하이라이트 세부 정보]
     * 특정 날짜나 사건을 AI가 특별하게 인식하여 생성한 요약 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "AI 분석 하이라이트 세부 정보")
    public static class HighlightInfo {
        @Schema(description = "하이라이트 소제목", example = "한강 공원에서의 첫 질주")
        private String title;

        @Schema(description = "하이라이트 상세 내용", example = "처음으로 리드줄 없이 운동장을 뛰어놀며 무척 행복해 보였던 날입니다.")
        private String content;
    }
}