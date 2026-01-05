package com.petlog.record.repository.jpa;

import com.petlog.record.entity.Diary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DiaryRepository extends JpaRepository<Diary, Long> {
    // 특정 사용자가 작성한 모든 일기 목록 조회
    List<Diary> findAllByUserId(Long userId);

    // 특정 펫에 대해 작성된 모든 일기 목록 조회
    List<Diary> findAllByPetId(Long petId);

    // AI 리캡 조회를 위해 추가 (필드명이 date일 경우)
    List<Diary> findAllByPetIdAndDateBetween(Long petId, LocalDate start, LocalDate end);

    /**
     * 특정 사용자가 특정 펫의 일기를 작성한 적이 있는지 확인합니다.
     * 리캡 예약 시 소유권 및 기록 여부를 검증하기 위해 사용됩니다.
     */
    boolean existsByPetIdAndUserId(Long petId, Long userId);

    /**
     * 특정 사용자가 일기를 작성한 모든 펫의 ID 목록을 조회합니다.
     * [추가됨] 펫이 여러 마리일 때 모든 펫에 대해 리캡을 예약하기 위함입니다.
     */
    @Query("SELECT DISTINCT d.petId FROM Diary d WHERE d.userId = :userId")
    List<Long> findDistinctPetIdsByUserId(@Param("userId") Long userId);
}