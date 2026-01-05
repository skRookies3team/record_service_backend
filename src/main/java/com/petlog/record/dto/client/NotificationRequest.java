package com.petlog.record.dto.client;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {
    private String type;        // "DIARY", "RECAP"
    private Long senderId;      // 사용자 ID
    private Long receiverId;    // 알림 받을 사용자 ID
    private Long targetId;      // 일기/리캡 ID
    private Long coin;          // 선택 (코인 적립 시)
}