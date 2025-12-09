package com.petlog.record.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// Pet Service 호출
@FeignClient(name = "pet-service", url = "${external.pet-service.url}")
public interface PetServiceClient {
    @GetMapping("/api/pets/{petId}/exists")
    boolean checkPetExists(@PathVariable("petId") Long petId);
}
