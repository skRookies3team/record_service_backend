package com.petlog.record.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Getter
@NoArgsConstructor
@ToString
public class AiDiaryRequest {

    @JsonProperty("userId")
    private Long userId;

    @JsonProperty("petId")
    private Long petId;

    // AI에게 강조하고 싶은 키워드나 사용자가 미리 작성한 메모
    @JsonProperty("content")
    private String content;

    @JsonProperty("latitude")
    private Double latitude;

    @JsonProperty("longitude")
    private Double longitude;

    // ✅ AI 미리보기 기준 날짜 추가
    @JsonProperty("date")
    private String date;

    // 이미 보관함에 있는 사진을 선택했을 경우의 리스트
    @JsonProperty("images")
    private List<DiaryRequest.Image> images;
}