package com.petlog.record.service;

import com.petlog.record.dto.client.ArchiveResponse;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

public interface DiaryMediaService {
    ArchiveResponse.CreateArchiveDtoList uploadToArchive(Long userId, List<MultipartFile> files);
    void savePhotoMetadata(Long imageId, Map<String, Object> metadata);
    Map<Long, Map<String, Object>> getMetadataMap(List<Long> imageIds);
    void deleteMetadataByImageIds(List<Long> imageIds);
}