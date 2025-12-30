package com.petlog.record.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Topic 설정
 * * WHY 필요?
 * - 기본적으로 자동 생성되는 토픽은 파티션이 1개임
 * - 처리량(Throughput) 증대 및 병렬 처리를 위해 파티션을 3개로 명시적 설정
 */
@Configuration
public class KafkaTopicConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    public static final String DIARY_EVENTS = "diary-events";

    /**
     * KafkaAdmin 설정
     * NewTopic 빈을 읽어 실제 브로커에 토픽을 생성하는 역할
     */
    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    /**
     * diary-events 토픽 정의
     * 파티션 3개, 복제본 1개(로컬) 설정
     */
    @Bean
    public NewTopic diaryEventsTopic() {
        return TopicBuilder.name(DIARY_EVENTS)
                .partitions(3)    // 파티션을 3개로 지정
                .replicas(1)      // 로컬 환경이므로 1로 설정
                .build();
    }
}