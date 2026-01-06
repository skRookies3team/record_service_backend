package com.petlog.record.repository.jpa;

import com.petlog.record.entity.Diary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * [다이어리 조회 전용 리포지토리]
 * 캘린더 뷰, AI 보관함 등 특정 UI 구성에 최적화된 읽기 전용 쿼리를 수행
 */
@Repository
public interface DiaryQueryRepository extends JpaRepository<Diary, Long> {

    /** [캘린더용 날짜별 조회] 특정 사용자가 특정 날짜에 작성한 모든 일기 목록을 반환 */
    List<Diary> findAllByUserIdAndDate(Long userId, LocalDate date);

    /** [AI 보관함 조회] AI 초안 기능을 통해 생성된 일기들만 최신순으로 필터링 */
    List<Diary> findAllByUserIdAndIsAiGenOrderByCreatedAtDesc(Long userId, Boolean isAiGen);
}
