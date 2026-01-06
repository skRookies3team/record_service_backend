package com.petlog.record.client;

import com.petlog.record.dto.client.NotificationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * [알림 서비스 외부 API 호출]
 * 알림 서비스(notification-service)와 통신하여 사용자에게 시스템 알림을 전달하기 위한 FeignClient
 */
@FeignClient(name = "notification-service", url = "${external.notification-service.url}")
public interface NotificationClient {

    /**
     * [알림 생성 및 발송 API 호출]
     * 알림 서비스의 NotificationController.createNotification을 호출
     * 리캡 완료, 코인 적립 등 서비스 내 주요 이벤트 발생 시 알림 데이터를 생성하고 전송함
     */
    @PostMapping("/api/notifications/create")
    void createNotification(@RequestBody NotificationRequest request);
}