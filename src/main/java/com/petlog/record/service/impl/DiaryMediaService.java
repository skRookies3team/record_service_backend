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
 * [미디어 데이터 통합 관리 서비스]
 * 외부 이미지 서버(S3 업로드) 연동 및 MongoDB를 통한 사진 비정형 메타데이터 관리 수행
 * RDB(PostgreSQL)와 NoSQL(MongoDB) 사이의 데이터 일관성을 조율하는 역할
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiaryMediaService {

    private final ImageClient imageClient;
    private final PhotoMetadataRepository photoMetadataRepository;

    /**
     * [이미지 서버 업로드]
     * 사용자가 업로드한 원본 파일을 외부 이미지 서비스(user-service)로 전달하여 S3 저장 및 보관함 등록 수행
     * @param userId 사용자 ID
     * @param files 업로드 대상 파일 리스트
     * @return 업로드된 이미지 정보 및 보관함 ID 목록
     */
    public ArchiveResponse.CreateArchiveDtoList uploadToArchive(Long userId, List<MultipartFile> files) {
        try {
            return imageClient.createArchive(userId, files);
        } catch (Exception e) {
            log.error("Image Upload Failed", e);
            throw new RuntimeException("이미지 서버 연동 실패");
        }
    }

    /**
     * [사진 메타데이터 저장 (MongoDB)]
     * 사진의 EXIF 정보나 AI가 분석한 객체 태그 등 비정형 데이터를 MongoDB에 저장
     * @param imageId PostgreSQL에 저장된 이미지 엔티티의 ID (연결 고리)
     * @param metadata 저장할 비정형 데이터 맵
     */
    public void savePhotoMetadata(Long imageId, Map<String, Object> metadata) {
        PhotoMetadata mongoData = PhotoMetadata.builder()
                .imageId(imageId)
                .metadata(metadata)
                .build();
        photoMetadataRepository.save(mongoData);
    }

    /**
     * [다중 메타데이터 일괄 조회]
     * 일기 상세 조회 시 여러 이미지의 메타데이터를 효율적으로 가져오기 위해 Map 구조로 반환
     * @param imageIds 조회할 이미지 ID 리스트
     * @return ImageId를 Key로, 메타데이터 Map을 Value로 가지는 Map
     */
    public Map<Long, Map<String, Object>> getMetadataMap(List<Long> imageIds) {
        return photoMetadataRepository.findAllByImageIdIn(imageIds)
                .stream()
                .collect(Collectors.toMap(
                        PhotoMetadata::getImageId,
                        PhotoMetadata::getMetadata,
                        (existing, replacement) -> existing // 중복 ID 발생 시 기존 데이터 유지
                ));
    }

    /**
     * [메타데이터 일괄 삭제]
     * 일기 삭제 시 해당 이미지들의 비정형 메타데이터도 MongoDB에서 함께 제거
     * @param imageIds 삭제 대상 이미지 ID 리스트
     */
    public void deleteMetadataByImageIds(List<Long> imageIds) {
        photoMetadataRepository.deleteAllByImageIdIn(imageIds);
    }
}