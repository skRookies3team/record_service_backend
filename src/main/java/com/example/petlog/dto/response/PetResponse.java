package com.example.petlog.dto.response;

import com.example.petlog.entity.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

public class PetResponse {

    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Getter
    public static class CreatePetDto {
        //펫 id
        private Long petId;

        //펫 이름
        private String petName;

        //생성일
        private LocalDateTime createdAt;

        public static PetResponse.CreatePetDto fromEntity(Pet pet) {

            return PetResponse.CreatePetDto.builder()
                    .petId(pet.getId())
                    .petName(pet.getPetName())
                    .createdAt(pet.getCreatedAt())
                    .build();
        }

    }
}
