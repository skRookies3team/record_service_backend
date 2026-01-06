package com.petlog.record.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * [데이터베이스 멀티 저장소 설정]
 * JPA(RDB)와 MongoDB(NoSQL)의 리포지토리 경로를 분리하여
 * 데이터 성격에 맞는 멀티 DB 환경을 구성하는 설정 클래스
 */
@Configuration

/*
 * [JPA 리포지토리 활성화]
 * 관계형 데이터베이스 처리를 위한 리포지토리 경로 지정
 * 주로 유저, 펫 프로필 등 정형 데이터를 관리
 */
@EnableJpaRepositories(
    basePackages = "com.petlog.record.repository.jpa"
)

/*
 * [MongoDB 리포지토리 활성화]
 * 문서 지향 NoSQL 처리를 위한 리포지토리 경로 지정
 * 주로 AI 일기, 로그 등 비정형/대용량 데이터를 관리
 */
@EnableMongoRepositories(
    basePackages = "com.petlog.record.repository.mongo"
)
public class DbConfig {

}