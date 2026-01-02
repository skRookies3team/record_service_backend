package com.petlog.record.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
// 1. JPA 리포지토리 경로 지정
@EnableJpaRepositories(
    basePackages = "com.petlog.record.repository.jpa"
)
// 2. MongoDB 리포지토리 경로 지정
@EnableMongoRepositories(
    basePackages = "com.petlog.record.repository.mongo"
)
public class DbConfig {
    // 특별한 빈 설정이 없다면 클래스 몸체는 비워두어도 됩니다.
}