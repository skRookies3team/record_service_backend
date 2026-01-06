package com.petlog.record.service.impl;

import com.petlog.record.entity.Diary;
import io.milvus.client.MilvusClient;
import io.milvus.param.R;
import io.milvus.param.collection.FlushParam;
import io.milvus.param.collection.HasCollectionParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * [벡터 DB(Milvus) 관리 서비스]
 * 일기 본문을 임베딩(Embedding)하여 벡터 저장소에 적재함으로써
 * 시맨틱 검색(Semantic Search) 및 RAG(검색 증강 생성) 기술의 기반을 제공
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiaryVectorService {

    private final VectorStore vectorStore;
    private final MilvusClient milvusClient;

    @Value("${spring.ai.vectorstore.milvus.collection-name:vector_store}")
    private String collectionName;

    /**
     * [일기 데이터 벡터화 및 저장]
     * 일기 본문과 메타데이터를 결합하여 벡터 저장소(Milvus)에 비동기로 저장
     * 저장 직후 Flush를 통해 데이터의 즉각적인 가시성(Visibility)을 확보함
     * * @param diary 저장 대상 일기 엔티티
     */
    @Async
    public void saveToVectorDB(Diary diary) {
        try {
            log.info("Milvus Vector DB 저장 시작 - DiaryId: {}", diary.getDiaryId());

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("userId", diary.getUserId());
            metadata.put("petId", diary.getPetId());
            metadata.put("diaryId", diary.getDiaryId());
            metadata.put("title", diary.getTitle());
            metadata.put("date", diary.getDate().toString());
            metadata.put("mood", diary.getMood());
            if (diary.getWeather() != null) metadata.put("weather", diary.getWeather());
            if (diary.getLocationName() != null) metadata.put("location", diary.getLocationName());

            Document document = new Document(diary.getContent(), metadata);
            vectorStore.add(List.of(document));

            if (hasCollection(collectionName)) {
                milvusClient.flush(FlushParam.newBuilder()
                        .addCollectionName(collectionName)
                        .build());
            }
            log.info("Milvus 저장 및 Flush 완료");
        } catch (Exception e) {
            log.error("Milvus 저장 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * [컬렉션 존재 여부 확인]
     * Milvus 내 지정된 컬렉션이 생성되어 있는지 확인
     */
    private boolean hasCollection(String name) {
        try {
            R<Boolean> response = milvusClient.hasCollection(HasCollectionParam.newBuilder()
                    .withCollectionName(name)
                    .build());
            return response.getStatus() == R.Status.Success.getCode() && Boolean.TRUE.equals(response.getData());
        } catch (Exception e) {
            log.error("Milvus 컬렉션 확인 오류: {}", e.getMessage());
            return false;
        }
    }
}