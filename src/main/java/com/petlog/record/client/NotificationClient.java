package com.petlog.record.client;

import com.petlog.record.dto.client.NotificationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

// Notification service 호출
@FeignClient(name = "notification-service", url = "${external.notification-service.url}")
public interface NotificationClient {

    @PostMapping("/api/notifications/create")
    void createNotification(@RequestBody NotificationRequest request);
}