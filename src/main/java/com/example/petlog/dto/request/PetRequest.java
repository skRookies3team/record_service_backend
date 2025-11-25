package com.example.petlog.dto.request;

import com.example.petlog.entity.GenderType;
import com.example.petlog.entity.Species;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

public class PetRequest {

    @Getter
    @Setter
    @NoArgsConstructor
    public static class CreatePetDto {

        //펫 이름(이름, 품종, 나이, 성별)
        //산책 횟수, 친구수, 사진 개수, 몸무게, 성격, 건강상태(예방접종여부, 체중 상태)
        @NotNull
        private String petName;

        //종류
        @NotNull
        private Species species;

        //품종
        @NotNull
        private String breed;

        //성별
        @NotNull
        private GenderType genderType;

        //중성화여부
        @NotNull
        private boolean isNeutered;

        //프로필 사진
        private String profileImage;

        //나이
        @NotNull
        private Integer age;

        //생일
        @NotNull
        private LocalDateTime birth;
    }
}
