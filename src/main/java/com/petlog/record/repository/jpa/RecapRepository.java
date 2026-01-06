package com.petlog.record.repository.jpa;

import com.petlog.record.entity.Recap;
import com.petlog.record.entity.RecapStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * [AI 리캡 리포지토리]
 * 월간 요약 데이터(Recap)의 이력을 관리하고 생성 상태를 추적
 */
@Repository
public interface RecapRepository extends JpaRepository<Recap, Long> {

    /** [사용자 리캡 히스토리] 특정 유저의 모든 리캡 목록을 최신순으로 제공 */
    List<Recap> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    /** [펫별 리캡 히스토리] 특정 펫의 성장 기록(리캡)을 최신순으로 제공 */
    List<Recap> findAllByPetIdOrderByCreatedAtDesc(Long petId);

    /** [스케줄러용] 현재 처리 대기(WAITING) 중인 리캡 목록을 조회 */
    List<Recap> findAllByStatus(RecapStatus status);

}