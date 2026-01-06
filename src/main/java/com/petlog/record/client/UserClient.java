package com.petlog.record.client;

import com.petlog.record.dto.client.UserClientResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * [유저 서비스 외부 API 호출]
 * 유저 서비스(user-service)와 통신하여 사용자 검증 및 보상(코인) 처리를 수행하기 위한 FeignClient
 */
@FeignClient(name = "user-service", url = "${external.user-service.url}")
public interface UserClient {

    /**
     * [사용자 존재 여부 확인 API 호출]
     * 유저 서비스의 UserController.checkUserExists를 호출
     * 특정 사용자 ID의 유효성을 검증하여 정상적인 요청인지 확인
     */
    @GetMapping("/api/users/{userId}/exists")
    Boolean checkUserExists(@PathVariable("userId") Long userId);

    /**
     * [사용자 상세 정보 조회 API 호출]
     * 유저 서비스의 UserController.getUserInfo를 호출
     * 사용자의 닉네임, 프로필 등 유저 관련 메타데이터를 조회
     */
    @GetMapping("/api/users/{userId}")
    UserClientResponse getUserInfo(@PathVariable("userId") Long userId);

    /**
     * [코인 적립 API 호출]
     * 유저 서비스의 CoinController.earnCoin을 호출
     * AI 리캡 완료 등 서비스 활동 보상으로 코인을 적립 처리
     */
    @PostMapping("/api/users/{userId}/coin/earn")
    void earnCoin(@PathVariable("userId") Long userId, @RequestBody Map<String, Object> request);
}