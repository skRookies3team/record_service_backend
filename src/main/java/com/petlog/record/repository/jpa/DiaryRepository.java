package com.petlog.record.repository.jpa;

import com.petlog.record.entity.Diary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * [다이어리 핵심 리포지토리]
 * 다이어리의 기본 CRUD 및 AI 리캡 집계를 위한 도메인 비즈니스 쿼리를 담당
 */
@Repository
public interface DiaryRepository extends JpaRepository<Diary, Long> {

    /** [리캡 분석용 조회] 특정 기간 동안 특정 반려동물의 일기 데이터를 수집 */
    List<Diary> findAllByPetIdAndDateBetween(Long petId, LocalDate start, LocalDate end);

    /** [권한 검증] 사용자가 해당 펫의 일기를 작성할 권한(기록 이력)이 있는지 확인 */
    boolean existsByPetIdAndUserId(Long petId, Long userId);

    /** [일괄 처리용] 일기 기록이 존재하는 모든 펫의 식별자 목록을 중복 없이 조회 */
    @Query("SELECT DISTINCT d.petId FROM Diary d WHERE d.userId = :userId")
    List<Long> findDistinctPetIdsByUserId(@Param("userId") Long userId);
}