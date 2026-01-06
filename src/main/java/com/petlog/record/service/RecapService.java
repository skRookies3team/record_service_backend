package com.petlog.record.service;

import com.petlog.record.dto.request.RecapRequest;
import com.petlog.record.dto.response.RecapResponse;
import java.util.List;

/**
 * [월간 리캡 통합 서비스 인터페이스]
 * AI 분석 결과를 도메인 엔티티와 결합하고,
 * 생성 대기(WAITING) 및 완료(GENERATED) 상태의 생명주기를 관리함
 */
public interface RecapService {
    /**
     * [AI 리캡 즉시 생성]
     * 수집된 데이터를 바탕으로 AI 분석을 수행하고 최종 리캡 엔티티를 생성 및 저장
     * @param request 생성 요청 정보 (펫ID, 기간 등)
     * @return 생성된 리캡 고유 ID
     */
    Long createAiRecap(RecapRequest.Generate request);

    /**
     * [리캡 생성 예약]
     * 사용자가 리캡 생성을 요청했으나 데이터 부족 등으로 즉시 생성이 어려울 때
     * 혹은 스케줄러 처리를 위해 '대기(WAITING)' 상태로 선 저장
     */
    Long createWaitingRecap(RecapRequest.Create request);

    /**
     * [리캡 상세 정보 조회]
     * 특정 리캡의 상세 내용을 반환하며, 요청한 사용자가 소유자인지 보안 검증을 수행
     * @param recapId 리캡 식별자
     * @param userId 요청자 식별자 (권한 확인용)
     */
    RecapResponse.Detail getRecap(Long recapId, Long userId);

    /** [사용자 리캡 목록 조회] 특정 사용자가 보유한 모든 반려동물의 리캡 히스토리를 반환 */
    List<RecapResponse.Simple> getAllRecaps(Long userId);

    /** [반려동물별 리캡 조회] 특정 반려동물 한 마리에 대해 생성된 리캡 목록을 반환 */
    List<RecapResponse.Simple> getRecapsByPet(Long petId);
}