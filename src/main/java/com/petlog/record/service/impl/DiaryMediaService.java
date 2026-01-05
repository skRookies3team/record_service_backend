package com.petlog.record.service.impl;

import com.petlog.record.client.ImageClient;
import com.petlog.record.dto.client.ArchiveResponse;
import com.petlog.record.entity.mongo.PhotoMetadata;
import com.petlog.record.repository.mongo.PhotoMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 이미지 및 미디어 데이터 전담 서비스
 * 이미지 서버 업로드 및 MongoDB 사진 메타데이터 관리 담당
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiaryMediaService {

    private final ImageClient imageClient;
    private final PhotoMetadataRepository photoMetadataRepository;

    public ArchiveResponse.CreateArchiveDtoList uploadToArchive(Long userId, List<MultipartFile> files) {
        try {
            return imageClient.createArchive(userId, files);
        } catch (Exception e) {
            log.error("Image Upload Failed", e);
            throw new RuntimeException("이미지 서버 연동 실패");
        }
    }

    public void savePhotoMetadata(Long imageId, Map<String, Object> metadata) {
        PhotoMetadata mongoData = PhotoMetadata.builder()
                .imageId(imageId)
                .metadata(metadata)
                .build();
        photoMetadataRepository.save(mongoData);
    }

    public Map<Long, Map<String, Object>> getMetadataMap(List<Long> imageIds) {
        return photoMetadataRepository.findAllByImageIdIn(imageIds)
                .stream()
                .collect(Collectors.toMap(
                        PhotoMetadata::getImageId,
                        PhotoMetadata::getMetadata,
                        (existing, replacement) -> existing
                ));
    }

    public void deleteMetadataByImageIds(List<Long> imageIds) {
        photoMetadataRepository.deleteAllByImageIdIn(imageIds);
    }
}