package com.example.petlog.service;

import com.example.petlog.dto.response.PhotoArchiveResponse;
import com.example.petlog.entity.PhotoArchive;

import java.util.List;

public interface PhotoArchiveService {
    
    // 사진 목록 저장 (DiaryService 등에서 호출)
    void saveArchives(List<PhotoArchive> archives);

    // 사용자별 보관함 조회
    List<PhotoArchiveResponse> getPhotoArchives(Long userId);
}