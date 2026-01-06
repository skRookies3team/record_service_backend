package com.petlog.record.dto.client;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * [알림 생성 요청 DTO]
 * 알림 서비스(Notification Service)로 알림 발송을 요청할 때 필요한 데이터를 담는 객체
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {
    private String type;        // "DIARY", "RECAP"
    private Long senderId;      // 알림을 보내는 주체 (이벤트를 발생시킨 사용자 ID)
    private Long receiverId;    // 알림을 받을 수신자 ID
    private Long targetId;      // 일기/리캡 ID
    private Long coin;          // 선택 (코인 적립 시)
}