//package com.petlog.record.dto;
//
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//import java.time.LocalDateTime;
//
///**
// * Diary Event Message (Kafka)
// *
// * Diary 생성/수정/삭제 이벤트를 Healthcare Service에 전달
// *
// * WHY 필요?
// * - Healthcare Service가 RAG를 위해 Diary를 벡터화
// * - Milvus Vector DB에 저장하여 Persona Chat에 사용
// *
// * @author diary-team
// * @since 2025-12-23
// */
//@Data
//@Builder
//@NoArgsConstructor
//@AllArgsConstructor
//public class DiaryEventMessage {
//
//    /**
//     * 이벤트 타입
//     * - DIARY_CREATED: 다이어리 생성
//     * - DIARY_UPDATED: 다이어리 수정
//     * - DIARY_DELETED: 다이어리 삭제
//     */
//    private String eventType;
//
//    /**
//     * Diary ID (Primary Key)
//     */
//    private Long diaryId;
//
//    /**
//     * 사용자 ID
//     */
//    private Long userId;
//
//    /**
//     * 반려동물 ID
//     */
//    private Long petId;
//
//    /**
//     * Diary 내용 (AI 생성 또는 사용자 작성)
//     * Healthcare Service가 이 내용을 벡터화
//     */
//    private String content;
//
//    /**
//     * Diary 이미지 URL (S3)
//     */
//    private String imageUrl;
//
//    /**
//     * Diary 생성 시간
//     */
//    private LocalDateTime createdAt;
//}
