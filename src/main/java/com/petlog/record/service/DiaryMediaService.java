package com.petlog.record.service;

import com.petlog.record.dto.client.ArchiveResponse;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

/**
 * [미디어 및 메타데이터 관리 서비스 인터페이스]
 * 외부 이미지 서버(S3) 업로드 및 MongoDB를 통한 사진 비정형 메타데이터(EXIF 등) 관리를 담당
 * RDB(PostgreSQL)와 NoSQL(MongoDB) 간의 데이터 브릿지 역할 수행
 */
public interface DiaryMediaService {

    /**
     * [이미지 서버 업로드 및 보관함 등록]
     * 사용자가 업로드한 멀티파트 파일을 외부 이미지 서비스로 전달하여 저장
     * @param userId 사용자 식별자
     * @param files 업로드 대상 이미지 파일 리스트
     * @return 생성된 보관함(Archive) 정보 목록
     */
    ArchiveResponse.CreateArchiveDtoList uploadToArchive(Long userId, List<MultipartFile> files);

    /**
     * [사진 상세 메타데이터 저장]
     * 사진의 기술적 정보(EXIF, 위치 등)나 AI 분석 태그를 MongoDB에 비정형 데이터로 저장
     * @param imageId PostgreSQL의 DiaryImage ID (RDB-NoSQL 매핑 키)
     * @param metadata 저장할 Key-Value 형태의 메타데이터
     */
    void savePhotoMetadata(Long imageId, Map<String, Object> metadata);

    /**
     * [다중 메타데이터 일괄 조회]
     * 일기 상세 조회 시 여러 이미지의 메타데이터를 효율적으로 가져오기 위한 맵 조회
     * @param imageIds 조회할 이미지 식별 ID 리스트
     * @return ImageId를 Key로 하는 메타데이터 맵
     */
    Map<Long, Map<String, Object>> getMetadataMap(List<Long> imageIds);

    /**
     * [메타데이터 일괄 삭제]
     * 일기 삭제 시 연관된 MongoDB 내의 메타데이터를 함께 제거하여 데이터 정합성 유지
     * @param imageIds 삭제 대상 이미지 ID 리스트
     */
    void deleteMetadataByImageIds(List<Long> imageIds);
}