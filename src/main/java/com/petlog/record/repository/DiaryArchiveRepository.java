package com.petlog.record.repository;

import com.petlog.record.entity.DiaryArchive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DiaryArchiveRepository extends JpaRepository<DiaryArchive, Long> {
    // 필요한 경우 특정 다이어리에 속한 매핑 삭제 등의 메서드를 추가할 수 있습니다.
}