//package com.petlog.record.controller;
//
//import com.petlog.record.infrastructure.kafka.DiaryEventProducer;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
///**
// * Kafka 테스트용 Controller
// *
// * 개발 중에만 사용, 프로덕션에서는 제거
// *
// * @author diary-team
// * @since 2025-12-23
// */
//@RestController
//@RequestMapping("/api/test/kafka")
//@RequiredArgsConstructor
//public class DiaryKafkaTestController {
//
//    private final DiaryEventProducer diaryEventProducer;
//
//    /**
//     * Kafka 이벤트 발행 테스트
//     *
//     * POST /api/test/kafka/publish
//     * {
//     *   "diaryId": 1,
//     *   "userId": 123,
//     *   "petId": 456,
//     *   "content": "오늘 산책을 갔어요!",
//     *   "imageUrl": "https://s3.../image.jpg"
//     * }
//     */
//    @PostMapping("/publish")
//    public ResponseEntity<String> publishTestEvent(@RequestBody TestEventRequest request) {
//        diaryEventProducer.publishDiaryCreatedEvent(
//                request.diaryId(),
//                request.userId(),
//                request.petId(),
//                request.content(),
//                request.imageUrl()
//        );
//
//        return ResponseEntity.ok("Event published to Kafka!");
//    }
//
//    public record TestEventRequest(
//            Long diaryId,
//            Long userId,
//            Long petId,
//            String content,
//            String imageUrl
//    ) {}
//}
