package com.petlog.record.infrastructure.kafka;

import com.petlog.record.dto.DiaryEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * Diary Event Kafka Producer
 *
 * Diary 생성/수정/삭제 이벤트를 Healthcare Service로 발행
 *
 * @author diary-team
 * @since 2025-12-23
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DiaryEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC = "diary-events";

    /**
     * Diary 생성 이벤트 발행
     */
    public void publishDiaryCreatedEvent(
            Long diaryId,
            Long userId,
            Long petId,
            String content,
            String imageUrl
    ) {
        log.info("Publishing DIARY_CREATED event - diaryId: {}", diaryId);

        DiaryEventMessage message = DiaryEventMessage.builder()
                .eventType("DIARY_CREATED")
                .diaryId(diaryId)
                .userId(userId)
                .petId(petId)
                .content(content)
                .imageUrl(imageUrl)
                .createdAt(LocalDateTime.now())
                .build();

        // 비동기 발행
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(TOPIC, userId.toString(), message);

        // 콜백 처리
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("✅ Event published successfully - diaryId: {}, partition: {}, offset: {}",
                        diaryId,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("❌ Failed to publish event - diaryId: {}", diaryId, ex);
                // TODO: 실패 시 DB에 실패 로그 저장 또는 재시도 큐에 추가
            }
        });
    }

    /**
     * Diary 수정 이벤트 발행
     */
    public void publishDiaryUpdatedEvent(
            Long diaryId,
            Long userId,
            Long petId,
            String content
    ) {
        log.info("Publishing DIARY_UPDATED event - diaryId: {}", diaryId);

        DiaryEventMessage message = DiaryEventMessage.builder()
                .eventType("DIARY_UPDATED")
                .diaryId(diaryId)
                .userId(userId)
                .petId(petId)
                .content(content)
                .createdAt(LocalDateTime.now())
                .build();

        kafkaTemplate.send(TOPIC, userId.toString(), message);
    }

    /**
     * Diary 삭제 이벤트 발행
     */
    public void publishDiaryDeletedEvent(
            Long diaryId,
            Long userId,
            Long petId
    ) {
        log.info("Publishing DIARY_DELETED event - diaryId: {}", diaryId);

        DiaryEventMessage message = DiaryEventMessage.builder()
                .eventType("DIARY_DELETED")
                .diaryId(diaryId)
                .userId(userId)
                .petId(petId)
                .createdAt(LocalDateTime.now())
                .build();

        kafkaTemplate.send(TOPIC, userId.toString(), message);
    }
}
