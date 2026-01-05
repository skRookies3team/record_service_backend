package com.petlog.record.service;

import com.petlog.record.dto.response.RecapAiResponse;
import java.util.List;

public interface RecapAiService {
    /**
     * 반려동물의 이름과 일기 내용들을 바탕으로 한 달 리캡 데이터를 생성합니다.
     */
//    RecapAiResponse analyzeMonth(String petName, List<String> diaryEntries);

    /**
     * 특정 연도/월 정보를 포함하여 일기 내용을 분석합니다.
     */
    RecapAiResponse analyzeMonth(String petName, int year, int month, List<String> diaryEntries);
}