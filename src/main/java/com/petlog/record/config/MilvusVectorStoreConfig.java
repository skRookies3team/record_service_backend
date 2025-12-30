package com.petlog.record.config;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.collection.*;
import io.milvus.param.index.CreateIndexParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.MilvusVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Milvus Vector Store 설정
 *
 * WHY 필요?
 * - Spring AI의 자동 설정이 인덱스를 제대로 생성하지 못하는 문제 해결
 * - 명시적으로 컬렉션 생성 및 인덱스 설정
 *
 * @author diary-team
 * @since 2025-12-27
 */
@Slf4j
@Configuration
public class MilvusVectorStoreConfig {

    @Value("${spring.ai.vectorstore.milvus.client.host:localhost}")
    private String milvusHost;

    @Value("${spring.ai.vectorstore.milvus.client.port:19530}")
    private int milvusPort;

    @Value("${spring.ai.vectorstore.milvus.collection-name:vector_store}")
    private String collectionName;

    @Value("${spring.ai.vectorstore.milvus.embedding-dimension:1536}")
    private int embeddingDimension;

    /**
     * Milvus Client Bean
     */
    @Bean
    public MilvusServiceClient milvusServiceClient() {
        log.info("🔌 Milvus 연결 시작: {}:{}", milvusHost, milvusPort);

        MilvusServiceClient client = new MilvusServiceClient(
                ConnectParam.newBuilder()
                        .withHost(milvusHost)
                        .withPort(milvusPort)
                        .build()
        );

        log.info("✅ Milvus 연결 성공");

        // ✅ Bean 생성 직후 컬렉션 초기화
        initializeMilvusCollection(client);

        return client;
    }

    /**
     * Spring AI MilvusVectorStore Bean
     *
     * Spring AI 1.0.0-M4 생성자:
     * MilvusVectorStore(MilvusServiceClient, EmbeddingModel, boolean initializeSchema)
     */
    @Bean
    public MilvusVectorStore milvusVectorStore(
            MilvusServiceClient milvusClient,
            EmbeddingModel embeddingModel
    ) {
        log.info("📦 MilvusVectorStore Bean 생성 중...");
        log.info("   - Collection: {}", collectionName);
        log.info("   - Embedding Dimension: {}", embeddingDimension);

        // ✅ Spring AI 1.0.0-M4는 boolean initializeSchema만 받음
        // false = @PostConstruct에서 우리가 직접 초기화
        MilvusVectorStore vectorStore = new MilvusVectorStore(
                milvusClient,
                embeddingModel,
                false  // initializeSchema = false
        );

        // 컬렉션 이름 설정 (필요시)
        // vectorStore의 내부 설정은 @PostConstruct에서 처리

        return vectorStore;
    }

    /**
     * 컬렉션 초기화 (Bean 생성 시 호출)
     */
    private void initializeMilvusCollection(MilvusServiceClient client) {
        try {
            log.info("🚀 Milvus 컬렉션 초기화 시작: {}", collectionName);

            // 1. 기존 컬렉션이 있으면 삭제 (개발 환경용)
            if (hasCollection(client)) {
                log.warn("⚠️ 기존 컬렉션 발견 - 삭제 후 재생성");
                dropCollection(client);
            }

            // 2. 컬렉션 생성
            createCollection(client);

            // 3. 인덱스 생성
            createIndex(client);

            // 4. 컬렉션 로드
            loadCollection(client);

            log.info("✅ Milvus 초기화 완료");

        } catch (Exception e) {
            log.error("❌ Milvus 초기화 실패: {}", e.getMessage(), e);
            throw new RuntimeException("Milvus 초기화 실패", e);
        }
    }

    /**
     * 컬렉션 존재 여부 확인
     */
    private boolean hasCollection(MilvusServiceClient client) {
        HasCollectionParam param = HasCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .build();

        return client.hasCollection(param).getData();
    }

    /**
     * 컬렉션 삭제
     */
    private void dropCollection(MilvusServiceClient client) {
        DropCollectionParam param = DropCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .build();

        client.dropCollection(param);
        log.info("🗑️ 컬렉션 삭제 완료: {}", collectionName);
    }

    /**
     * 컬렉션 생성
     */
    private void createCollection(MilvusServiceClient client) {
        // 필드 스키마 정의
        FieldType idField = FieldType.newBuilder()
                .withName("id")
                .withDataType(io.milvus.grpc.DataType.Int64)
                .withPrimaryKey(true)
                .withAutoID(true)
                .build();

        FieldType contentField = FieldType.newBuilder()
                .withName("content")
                .withDataType(io.milvus.grpc.DataType.VarChar)
                .withMaxLength(65535)
                .build();

        FieldType metadataField = FieldType.newBuilder()
                .withName("metadata")
                .withDataType(io.milvus.grpc.DataType.JSON)
                .build();

        FieldType embeddingField = FieldType.newBuilder()
                .withName("embedding")
                .withDataType(io.milvus.grpc.DataType.FloatVector)
                .withDimension(embeddingDimension)
                .build();

        // 컬렉션 스키마 생성
        CreateCollectionParam param = CreateCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .withDescription("Diary entries for RAG system")
                .withFieldTypes(java.util.Arrays.asList(
                        idField, contentField, metadataField, embeddingField
                ))
                .build();

        client.createCollection(param);
        log.info("📝 컬렉션 생성 완료: {}", collectionName);
    }

    /**
     * 인덱스 생성
     */
    private void createIndex(MilvusServiceClient client) {
        CreateIndexParam param = CreateIndexParam.newBuilder()
                .withCollectionName(collectionName)
                .withFieldName("embedding")
                .withIndexType(IndexType.IVF_FLAT)
                .withMetricType(MetricType.COSINE)
                .withExtraParam("{\"nlist\":128}")
                .withSyncMode(Boolean.TRUE)
                .build();

        client.createIndex(param);
        log.info("🔍 인덱스 생성 완료: embedding field");
    }

    /**
     * 컬렉션 로드 (메모리에 적재)
     */
    private void loadCollection(MilvusServiceClient client) {
        LoadCollectionParam param = LoadCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .build();

        client.loadCollection(param);
        log.info("💾 컬렉션 로드 완료: {}", collectionName);
    }
}