package com.petlog.record.entity;

/**
 * [AI 리캡 처리 상태 타입]
 * 월간 리캡 생성 프로세스의 생명주기를 관리
 */
public enum RecapStatus {
    /** 분석 및 생성 완료: 사용자에게 노출 가능한 상태 */
    GENERATED,
    /** 생성 대기 중(카드 대응): 스케줄러에 의해 예약되었으나 AI 분석 전인 상태 */
    WAITING
}
