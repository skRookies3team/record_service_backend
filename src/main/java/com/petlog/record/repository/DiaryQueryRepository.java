package com.petlog.record.repository;

import com.petlog.record.entity.Diary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DiaryQueryRepository extends JpaRepository<Diary, Long> {

    // 1. 캘린더용 날짜별 조회
    List<Diary> findAllByUserIdAndDate(Long userId, LocalDate date);

    // 2. AI 다이어리 보관함 조회 (isAiGen = true 인 것만)
    List<Diary> findAllByUserIdAndIsAiGenOrderByCreatedAtDesc(Long userId, Boolean isAiGen);
}
