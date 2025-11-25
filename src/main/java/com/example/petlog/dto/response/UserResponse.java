package com.example.petlog.dto.response;

import com.example.petlog.entity.UserType;
import com.example.petlog.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

public class UserResponse {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateUserDto {

        private String email;

        private String username;

        private Long userId;

        private LocalDateTime createdAt;

        public static CreateUserDto fromEntity(User user) {

            return CreateUserDto.builder()
                    .userId(user.getId())
                    .email(user.getEmail())
                    .username(user.getUsername())
                    .createdAt(user.getCreatedAt())
                    .build();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LoginDto {
        private String token;
        private Long userId;
        private UserType role;
        private String email;
        private String username;

        public static LoginDto fromEntity(String token, UserResponse.AuthDto userDetails) {
            return UserResponse.LoginDto.builder()
                    .token(token)
                    .userId(userDetails.getUserId())
                    .role(userDetails.getType())
                    .email(userDetails.getEmail())
                    .username(userDetails.getUsername())
                    .build();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AuthDto {
        private Long userId;
        private String email;
        private String username;
        private String password;
        private UserType type;
        private String encryptedPwd;
        private LocalDateTime createdAt;

        public static AuthDto fromEntity(User user) {

            return AuthDto.builder()
                    .userId(user.getId())
                    .email(user.getEmail())
                    .username(user.getUsername())
                    .password(user.getPassword())
                    .type(user.getType())
                    .encryptedPwd(user.getEncryptedPwd())
                    .createdAt(user.getCreatedAt())
                    .build();
        }
    }

}
