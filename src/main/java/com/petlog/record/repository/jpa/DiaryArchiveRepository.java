package com.petlog.record.repository.jpa;

import com.petlog.record.entity.DiaryArchive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * [다이어리-보관함 매핑 리포지토리]
 * 일기 엔티티와 외부 이미지 서비스의 보관함 ID 간의 연결 데이터를 관리
 */
@Repository
public interface DiaryArchiveRepository extends JpaRepository<DiaryArchive, Long> {

}