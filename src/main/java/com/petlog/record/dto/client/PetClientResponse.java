package com.petlog.record.dto.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * [펫 서비스 응답 DTO]
 * 펫 서비스(pet-service)로부터 반려동물의 상세 프로필 정보를 받아오기 위한 객체
 * 기록 서비스 내에서 AI 일기 생성, 리캡 데이터 구성 시 펫의 메타데이터로 활용됨
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetClientResponse {

    // 펫 식별 고유 ID
    private Long petId;
    //펫 이름
    private String petName;
    //종류
    private Species species;
    //품종
    private String breed;
    //성별
    private GenderType genderType;
    //중성화여부
    private boolean is_neutered;
    //프로필 사진
    private String profileImage;
    //나이
    private Integer age;
    //생일
    private LocalDate birth;
    //상태
    private Status status;
}