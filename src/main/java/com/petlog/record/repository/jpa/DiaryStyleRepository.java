package com.petlog.record.repository.jpa;

import com.petlog.record.entity.DiaryStyle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

/**
 * [다이어리 스타일 설정 리포지토리]
 * 유저별/펫별/일기별로 개인화된 UI 스타일 설정값을 조회
 */
@Repository
public interface DiaryStyleRepository extends JpaRepository<DiaryStyle, Long> {

    /** [개별 일기 스타일] 특정 일기에 고유하게 적용된 스타일 조회 */
    Optional<DiaryStyle> findByDiaryId(Long diaryId);

    /** [펫별 스타일] 특정 반려동물 다이어리에 적용된 테마 설정 조회 */
    Optional<DiaryStyle> findByUserIdAndPetId(Long userId, Long petId);

}