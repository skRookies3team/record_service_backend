//package com.petlog.record.config;
//
//import com.petlog.record.dto.DiaryEventMessage;
//import org.apache.kafka.clients.producer.ProducerConfig;
//import org.apache.kafka.common.serialization.StringSerializer;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.kafka.core.DefaultKafkaProducerFactory;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.kafka.core.ProducerFactory;
//import org.springframework.kafka.support.serializer.JsonSerializer;
//
//import java.util.HashMap;
//import java.util.Map;
//
///**
// * Kafka Producer 설정
// *
// * WHY 필요?
// * - application.yml보다 세밀한 설정 가능
// * - 테스트 시 Mock으로 교체 용이
// *
// * @author diary-team
// * @since 2025-12-23
// */
//@Configuration
//public class KafkaProducerConfig {
//
//    @Value("${spring.kafka.bootstrap-servers}")
//    private String bootstrapServers;
//
//    /**
//     * Producer Factory 생성
//     *
//     * KafkaTemplate이 사용할 Producer 설정
//     */
//    @Bean
//    public ProducerFactory<String, Object> producerFactory() {
//        Map<String, Object> config = new HashMap<>();
//
//        // Kafka 서버 주소
//        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
//
//        // Key/Value Serializer
//        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
//        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
//
//        // 신뢰성 설정
//        config.put(ProducerConfig.ACKS_CONFIG, "all");
//        config.put(ProducerConfig.RETRIES_CONFIG, 3);
//
//        // 성능 설정
//        config.put(ProducerConfig.LINGER_MS_CONFIG, 10);
//        config.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
//        config.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
//
//        return new DefaultKafkaProducerFactory<>(config);
//    }
//
//    /**
//     * KafkaTemplate Bean
//     *
//     * Kafka 메시지 발행에 사용
//     */
//    /**
//     * KafkaTemplate Bean
//     * DiaryServiceImpl의 Required type인 KafkaTemplate<String, Object>와 일치시킵니다.
//     */
//    @Bean
//    public KafkaTemplate<String, Object> kafkaTemplate() { // ✅ DiaryEventMessage -> Object
//        return new KafkaTemplate<>(producerFactory());
//    }
//}
