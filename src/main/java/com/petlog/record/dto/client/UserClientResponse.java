package com.petlog.record.dto.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * [유저 서비스 응답 DTO]
 * 유저 서비스(user-service)로부터 사용자 프로필 및 계정 상태를 조회할 때 사용하는 객체
 * 기록 서비스의 작성자 정보 노출 및 사용자 기반 필터링에 활용됨
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserClientResponse {

    private String username;        // 사용자 이름(닉네임)
    private String genderType;      // 성별 (Enum -> String)
    private String profileImage;    // 프로필 사진
    private String statusMessage;   // 상태메세지
    private Integer age;            // 나이

}