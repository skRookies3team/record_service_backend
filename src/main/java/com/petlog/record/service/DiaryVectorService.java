package com.petlog.record.service;

import com.petlog.record.entity.Diary;

/**
 * [다이어리 벡터 동기화 서비스 인터페이스]
 * 검색 성능 고도화를 위해 일기 내용을 벡터(Vector) 데이터로 변환하여 전용 저장소에 동기화
 */
public interface DiaryVectorService {

    /** [벡터 DB 적재] 일기 생성/수정 시 텍스트 임베딩을 수행하여 Milvus 등에 저장 */
    void saveToVectorDB(Diary diary);
}