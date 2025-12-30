package com.petlog.record.config;

import org.apache.kafka.common.utils.Java;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(5)) // 연결 타임아웃 5초
                .readTimeout(Duration.ofSeconds(5))    // 읽기 타임아웃 5초
                .build();
    }
}

//RestTemplate 활용 시: 만약 Feign 대신 이미 주입된 RestTemplate을 쓴다면 아래와 같이 호출하세요.
//
//Java
//
//MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
//body.add("multipartFile", imageFile.getResource()); // 파일을 리소스 형태로 전달
//
//HttpHeaders headers = new HttpHeaders();
//headers.setContentType(MediaType.MULTIPART_FORM_DATA);
//
//HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
//ResponseEntity<List> response = restTemplate.postForEntity(userServiceUrl + "/api/images/upload", requestEntity, List.class);