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
     * 특정 기간 동안 일기를 작성한 적이 있는 펫의 ID와 사용자 ID의 고유 쌍을 조회합니다.
     * MSA 환경에서 외부 서비스의 Pet 테이블을 참조하지 않고 일기 기록만으로 대상자를 선정합니다.
     */
    @Query("SELECT DISTINCT d.petId, d.userId FROM Diary d WHERE d.date BETWEEN :start AND :end")
    List<Object[]> findDistinctPetAndUserByDateBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);
}