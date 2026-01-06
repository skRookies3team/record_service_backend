package com.petlog.record.service;

import com.petlog.record.dto.response.AiDiaryResponse;
import java.util.List;

/**
 * [AI 분석 전담 서비스 인터페이스]
 * 이미지 분석을 통한 일기 초안 생성 및 텍스트 데이터 추출을 담당
 * 멀티모달 LLM(Large Language Model)을 사용하여 사진의 맥락을 해석함
 */
public interface DiaryAiService {

    /**
     * [이미지 기반 일기 콘텐츠 생성]
     * 제공된 이미지 URL 리스트의 시각적 정보를 분석하여
     * 감성적인 제목, 본문, 날씨, 기분 등 일기 구성 요소를 자동으로 생성
     * @param imageUrls 분석 대상 이미지의 S3 전체 경로 리스트
     * @return AI 분석 결과가 담긴 구조화된 응답 객체
     */
    AiDiaryResponse generateContentWithAiFromUrls(List<String> imageUrls);
}