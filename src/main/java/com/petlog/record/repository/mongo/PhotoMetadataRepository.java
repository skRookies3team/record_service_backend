package com.petlog.record.repository.mongo;

import com.petlog.record.entity.mongo.PhotoMetadata; // MongoDB용 도큐먼트 객체
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PhotoMetadataRepository extends MongoRepository<PhotoMetadata, String> {
    // PostgreSQL의 DiaryImage ID로 상세 메타데이터를 찾아오는 기능

    // 1. 단일 조회 (이미지 ID로)
    Optional<PhotoMetadata> findByImageId(Long imageId);

    // 2. 다중 조회 (일기 조회 시 여러 이미지의 메타데이터를 한 번에 가져올 때)
    List<PhotoMetadata> findAllByImageIdIn(List<Long> imageIds);

    // 3. 다중 삭제 (일기 삭제 시 연관된 모든 메타데이터 삭제)
    // deleteAllBy... 또는 deleteBy... 모두 가능합니다.
    void deleteAllByImageIdIn(List<Long> imageIds);
}