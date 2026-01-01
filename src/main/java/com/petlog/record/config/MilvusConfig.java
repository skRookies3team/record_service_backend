package com.petlog.record.config;

import io.milvus.client.MilvusClient;
import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MilvusConfig {

    // application.yml에 설정된 milvus 접속 정보를 가져옵니다.
    // 설정이 없다면 기본값인 localhost:19530을 사용합니다.
    @Value("${spring.ai.vectorstore.milvus.client.host:localhost}")
    private String host;

    @Value("${spring.ai.vectorstore.milvus.client.port:19530}")
    private int port;

    /**
     * MilvusClient 빈 등록
     * 이 빈이 등록되어야 DiaryServiceImpl의 @RequiredArgsConstructor가 주입을 완료할 수 있습니다.
     */
    @Bean
    public MilvusClient milvusClient() {
        // Milvus 서버에 연결하기 위한 파라미터 설정
        ConnectParam connectParam = ConnectParam.newBuilder()
                .withHost(host)
                .withPort(port)
                .build();

        // MilvusServiceClient 인스턴스를 생성하여 반환합니다.
        return new MilvusServiceClient(connectParam);
    }
}