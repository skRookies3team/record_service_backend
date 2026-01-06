package com.petlog.record.repository.mongo;

import com.petlog.record.entity.mongo.PhotoMetadata; // MongoDB용 도큐먼트 객체
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/**
 * [사진 상세 메타데이터 리포지토리 - MongoDB]
 * PostgreSQL에 저장된 'DiaryImage'의 식별자(imageId)를 키로 사용하여
 * MongoDB에 저장된 비정형 사진 상세 정보(EXIF, AI 분석 결과 등)를 관리
 */
public interface PhotoMetadataRepository extends MongoRepository<PhotoMetadata, String> {

    /**
     * [단일 메타데이터 조회]
     * 특정 이미지 ID와 매핑된 단일 사진의 상세 비정형 데이터를 조회
     * @param imageId PostgreSQL에서 생성된 DiaryImage의 PK
     */
    Optional<PhotoMetadata> findByImageId(Long imageId);

    /**
     * [다중 메타데이터 조회]
     * 일기 상세 조회 시, 해당 일기에 포함된 모든 이미지의 메타데이터를 한 번에 조회
     * 여러 개의 imageId를 리스트로 받아 효율적인 배치 조회를 수행
     * @param imageIds 이미지 식별자 리스트
     */
    List<PhotoMetadata> findAllByImageIdIn(List<Long> imageIds);

    /**
     * [다중 메타데이터 삭제]
     * 일기 삭제 또는 이미지 일괄 삭제 시, MongoDB에 저장된 연관 메타데이터를 함께 제거
     * 데이터베이스 간의 논리적 삭제 일관성을 유지하기 위해 사용
     * @param imageIds 삭제 대상 이미지 식별자 리스트
     */
    void deleteAllByImageIdIn(List<Long> imageIds);
}