package com.petlog.record.service;

import com.petlog.record.dto.response.RecapAiResponse;
import java.util.List;

/**
 * [AI 월간 분석 엔진 명세]
 * 대량의 일기 텍스트(비정형 데이터)를 분석하여 구조화된 월간 요약 리포트를 생성
 * 프롬프트 엔지니어링을 통해 반려동물의 성장과 감정을 해석하는 역할을 수행
 */
public interface RecapAiService {

    /**
     * [월간 리캡 데이터 생성]
     * 반려동물의 정보와 한 달간의 일기 본문들을 LLM에 전달하여
     * 제목, 총평, 하이라이트가 포함된 분석 결과를 획득
     * @param petName 반려동물 명칭 (호칭 최적화용)
     * @param year 대상 연도
     * @param month 대상 월
     * @param diaryEntries 분석할 일기 텍스트 목록
     * @return AI가 생성한 구조화된 분석 응답 객체
     */
    RecapAiResponse analyzeMonth(String petName, int year, int month, List<String> diaryEntries);
}