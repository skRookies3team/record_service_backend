package com.petlog.record.scheduler;

import com.petlog.record.client.NotificationClient;
import com.petlog.record.dto.client.NotificationRequest;
import com.petlog.record.dto.request.RecapRequest;
import com.petlog.record.entity.Diary;
import com.petlog.record.entity.Recap;
import com.petlog.record.entity.RecapStatus;
import com.petlog.record.repository.jpa.RecapRepository;
import com.petlog.record.service.RecapService;
import com.petlog.record.repository.jpa.DiaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecapScheduler {

    private final RecapService recapService;
    private final DiaryRepository diaryRepository;
    private final RecapRepository recapRepository;
    private final NotificationClient notificationClient;  // 리캡 알림 연동

    /**
     * 테스트용: 1분마다 WAITING 상태 리캡을 확인하고 생성
     * 실제 운영에서는 매일 새벽 2시로 변경
     */
    @Scheduled(cron = "0 * * * * *")
    public void processWaitingRecaps() {
        log.info("[Scheduler] WAITING 상태 리캡 처리 시작");
        List<Recap> waitingRecaps = recapRepository.findAllByStatus(RecapStatus.WAITING);
        if (waitingRecaps.isEmpty()) {
            log.info("[Scheduler] 처리할 WAITING 리캡이 없습니다.");
            return;
        }
        log.info("[Scheduler] 총 {}개의 WAITING 리캡을 처리합니다.", waitingRecaps.size());
        for (Recap waitingRecap : waitingRecaps) {
            try {
                // 로그에서 확인할 수 있도록 변수 추출
                Long recapId = waitingRecap.getRecapId();
                Long petId = waitingRecap.getPetId();
                LocalDate start = waitingRecap.getPeriodStart();
                LocalDate end = waitingRecap.getPeriodEnd();

                // ✅ 수정된 로그: 어떤 펫의 어떤 기간을 뒤지고 있는지 확실히 알 수 있게 합니다.
                log.info("[Process] 리캡 ID: {} 분석 시도 - 대상 펫: {}, 조회 기간: {} ~ {}",
                        recapId, petId, start, end);
                //log.info("[Process] 리캡 ID: {} - AI 분석 시작", waitingRecap.getRecapId());
                // 기간 내 일기가 있는지 확인 (기존 메서드 재사용)
                List<Diary> diaries = diaryRepository.findAllByPetIdAndDateBetween(
                        waitingRecap.getPetId(),
                        waitingRecap.getPeriodStart(),
                        waitingRecap.getPeriodEnd()
                );
                if (diaries.isEmpty()) {
                    log.warn("[Skip] 리캡 ID: {} - 기간 내 일기 없음", waitingRecap.getRecapId());
                    continue;
                }
                // AI 리캡 생성 요청
                RecapRequest.Generate request = RecapRequest.Generate.builder()
                        .petId(waitingRecap.getPetId())
                        .userId(waitingRecap.getUserId())
                        .periodStart(waitingRecap.getPeriodStart())
                        .periodEnd(waitingRecap.getPeriodEnd())
                        .petName("우리 아이")
                        .build();
                // 기존 리캡을 삭제하고 새로 생성
                recapRepository.delete(waitingRecap); // WAITING 상태 리캡 삭제
                Long newRecapId = recapService.createAiRecap(request);  // 새로 생성 (GENERATED 상태)

                log.info("[Success] 리캡 ID: {} -> {} 로 생성 완료", waitingRecap.getRecapId(), newRecapId);

            } catch (Exception e) {
                log.error("[Error] 리캡 ID: {} 처리 실패", waitingRecap.getRecapId(), e);
            }
        }
        log.info("[Scheduler] WAITING 상태 리캡 처리 완료");
    }
}