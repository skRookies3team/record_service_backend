package com.petlog.record.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// User Service 호출
@FeignClient(name = "user-service", url = "${external.user-service.url}")
public interface UserServiceClient {
    @GetMapping("/api/users/{userId}/exists")
    boolean checkUserExists(@PathVariable("userId") Long userId);
}
