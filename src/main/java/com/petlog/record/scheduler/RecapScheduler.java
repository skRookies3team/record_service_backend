package com.petlog.record.scheduler;

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
                log.info("[Process] 리캡 ID: {} - AI 분석 시작", waitingRecap.getRecapId());
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
                recapRepository.delete(waitingRecap);
                Long newRecapId = recapService.createAiRecap(request);

                log.info("[Success] 리캡 ID: {} -> {} 로 생성 완료", waitingRecap.getRecapId(), newRecapId);
            } catch (Exception e) {
                log.error("[Error] 리캡 ID: {} 처리 실패", waitingRecap.getRecapId(), e);
            }
        }
        log.info("[Scheduler] WAITING 상태 리캡 처리 완료");
    }

    /**
     * 매월 1일 새벽 2시에 실행됩니다.
     * 지난 달에 일기 기록이 있는 펫들만 선별하여 리캡을 생성합니다.
     */
    @Scheduled(cron = "0 0 2 1 * *")
    //@Scheduled(cron = "1/30 *  * * * *")
    public void generateMonthlyRecaps() {
        log.info("[Batch] 정기 월간 리캡 자동 생성 프로세스 시작");

        LocalDate lastMonthStart = LocalDate.now().minusMonths(1).withDayOfMonth(1);
        LocalDate lastMonthEnd = lastMonthStart.withDayOfMonth(lastMonthStart.lengthOfMonth());

        // 1. 일기 기록이 있는 (petId, userId) 쌍을 조회
        List<Object[]> targetPairs = diaryRepository.findDistinctPetAndUserByDateBetween(lastMonthStart, lastMonthEnd);

        if (targetPairs.isEmpty()) {
            log.info("[Batch] 지난 달에 작성된 일기가 없어 생성 대상이 존재하지 않습니다.");
            return;
        }

        log.info("[Batch] 총 {}마리의 펫(기록 기준)에 대해 리캡 생성을 시작합니다.", targetPairs.size());

        // 2. 각 쌍에 대해 리캡 생성 수행
        for (Object[] pair : targetPairs) {
            Long petId = (Long) pair[0];
            Long userId = (Long) pair[1];

            try {
                log.info("[Process] 펫 ID: {} - 분석 시작", petId);

                RecapRequest.Generate request = RecapRequest.Generate.builder()
                        .petId(petId)
                        .userId(userId)
                        .periodStart(lastMonthStart)
                        .periodEnd(lastMonthEnd)
                        .petName("우리 아이") // MSA 구조상 이름을 알 수 없으므로 기본값 사용 (필요시 FeignClient로 호출)
                        .build();

                Long recapId = recapService.createAiRecap(request);
                log.info("[Success] 펫 ID: {} - 리캡 생성 완료 (ID: {})", petId, recapId);

            } catch (Exception e) {
                log.warn("[Skip] 펫 ID: {} - 생성 실패 (사유: {})", petId, e.getMessage());
            }
        }

        log.info("[Batch] 정기 월간 리캡 자동 생성 프로세스 완료");
    }
}