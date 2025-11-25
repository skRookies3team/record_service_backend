package com.example.petlog.dto.request;

import com.example.petlog.entity.GenderType;
import com.example.petlog.entity.Pet;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

public class UserRequest{
    @Getter
    @Setter
    @NoArgsConstructor
    public static class CreateUserDto {
        @NotNull
        private String email;
        @NotNull
        private String password;
        @NotNull
        private String username;
        @NotNull
        private Integer age;

        private String profileImage;

        @NotNull
        private GenderType genderType;

        //펫 정보
        private PetRequest.CreatePetDto pet;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    //로그인
    public static class LoginDto {

        @NotBlank(message = "아이디는 필수 입력 항목입니다.")
        private String email;

        @NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
        private String password;

    }
}