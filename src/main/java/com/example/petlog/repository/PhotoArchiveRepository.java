package com.example.petlog.repository;

import com.example.petlog.entity.PhotoArchive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhotoArchiveRepository extends JpaRepository<PhotoArchive, Long> {

    // 사용자별 사진 보관함 전체 조회 (최신순)
    List<PhotoArchive> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}