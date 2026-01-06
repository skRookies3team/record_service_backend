package com.petlog.record.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * [HTTP 통신 클라이언트 설정]
 * 외부 서비스 API 호출 및 마이크로서비스 간 동기 통신을 위한 RestTemplate 빈(Bean) 설정 클래스
 */
@Configuration
public class RestTemplateConfig {

    /**
     * [RestTemplate 빈 등록 및 타임아웃 설정]
     * 서비스 간 통신 시 무한 대기로 인한 리소스 고갈을 방지하기 위해
     * 연결(Connect) 및 읽기(Read) 타임아웃을 5초로 제한하여 가용성을 확보
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(5)) // 서버 연결 시도 제한 시간 : 연결 타임아웃 5초
                .readTimeout(Duration.ofSeconds(5))    // 응답 데이터를 읽는 제한 시간 : 읽기 타임아웃 5초
                .build();
    }
}