package com.petlog.record.scheduler;

import com.petlog.record.client.NotificationClient;
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

/**
 * [AI 리캡 생성 자동화 스케줄러]
 * 'WAITING' 상태로 예약된 리캡 데이터들을 주기적으로 스캔하여
 * AI 분석 엔진을 통해 최종 리캡 리포트(GENERATED)로 변환하는 작업을 수행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecapScheduler {

    private final RecapService recapService;
    private final DiaryRepository diaryRepository;
    private final RecapRepository recapRepository;
    private final NotificationClient notificationClient;  // 리캡 알림 연동

    /**
     * [예약된 리캡 일괄 처리 프로세스]
     * 1. DB에서 'WAITING' 상태의 리캡 목록을 조회
     * 2. 해당 기간 내 실제 일기 기록이 존재하는지 검증
     * 3. 조건을 충족할 경우 AI 분석을 호출하여 최종 리캡으로 갱신
     * * [스케줄링 정책]
     * - 현재(테스트): 매 1분마다 실행 (0 * * * * *)
     * - 운영 환경: 매일 새벽 2시 등 서버 부하가 적은 시간에 실행 권장
     */
    @Scheduled(cron = "0 * * * * *")
    public void processWaitingRecaps() {
        log.info("[Scheduler] WAITING 상태 리캡 처리 시작");

        // 1. 처리 대기 중인 리캡 목록 확보
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

                // 2. [데이터 검증] 분석 대상 기간 내 실제 작성된 일기가 있는지 확인
                List<Diary> diaries = diaryRepository.findAllByPetIdAndDateBetween(
                        waitingRecap.getPetId(),
                        waitingRecap.getPeriodStart(),
                        waitingRecap.getPeriodEnd()
                );
                if (diaries.isEmpty()) {
                    log.warn("[Skip] 리캡 ID: {} - 기간 내 일기 없음", waitingRecap.getRecapId());
                    continue;
                }

                // 3. [AI 생성 요청 전송] AI 분석을 위한 요청 DTO 구성
                RecapRequest.Generate request = RecapRequest.Generate.builder()
                        .petId(waitingRecap.getPetId())
                        .userId(waitingRecap.getUserId())
                        .periodStart(waitingRecap.getPeriodStart())
                        .periodEnd(waitingRecap.getPeriodEnd())
                        .petName("우리 아이")
                        .build();

                // 4. [데이터 전환] 기존 WAITING 예약을 삭제하고 AI가 분석한 GENERATED 리캡으로 교체
                recapRepository.delete(waitingRecap); // WAITING 상태 리캡 삭제
                Long newRecapId = recapService.createAiRecap(request);  // 새로 생성 (GENERATED 상태)

                log.info("[Success] 리캡 ID: {} -> {} 로 생성 완료", waitingRecap.getRecapId(), newRecapId);

            } catch (Exception e) {
                /* * [예외 처리]
                 * 개별 리캡 처리 중 오류 발생 시, 해당 항목만 로그를 남기고 다음 항목으로 진행하여
                 * 전체 배치 작업의 중단을 방지
                 */
                log.error("[Error] 리캡 ID: {} 처리 실패", waitingRecap.getRecapId(), e);
            }
        }
        log.info("[Scheduler] WAITING 상태 리캡 처리 완료");
    }
}