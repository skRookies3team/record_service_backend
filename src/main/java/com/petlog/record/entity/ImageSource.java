package com.petlog.record.entity;

/**
 * [이미지 출처 구분 타입]
 * 이미지의 유입 경로에 따라 외부 이미지 서비스(user-service) 전송 여부를 결정
 */
public enum ImageSource {
    GALLERY, // 외부 갤러리 (새로 업로드)
    ARCHIVE  // 내부 보관함 (기존 사진 선택)
}