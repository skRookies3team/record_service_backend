package com.petlog.record.client;

import com.petlog.record.dto.client.PetClientResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * [펫 서비스 외부 API 호출]
 * 펫 서비스(pet-service)와 통신하여 반려동물 정보 확인 및 상세 데이터를 조회하기 위한 FeignClient
 */
@FeignClient(name = "pet-service", url = "${PET_SERVICE_URL}")
public interface PetClient {

    /**
     * [펫 존재 여부 확인 API 호출]
     * 펫 서비스의 PetController.checkPetExists를 호출
     * 기록 생성 또는 리캡 작업 전, 대상 반려동물이 시스템에 존재하는지 검증
     */
    @GetMapping("/api/pets/{petId}/exists")
    Boolean checkPetExists(@PathVariable("petId") Long petId);

    /**
     * [펫 상세 정보 조회 API 호출]
     * 펫 서비스의 PetController.getPetInfo를 호출
     * 펫의 이름, 품종, 생일 등 AI 리캡 및 일기 생성에 필요한 정보를 조회
     */
    @GetMapping("/api/pets/{petId}")
    PetClientResponse getPetInfo(@PathVariable("petId") Long petId);
}