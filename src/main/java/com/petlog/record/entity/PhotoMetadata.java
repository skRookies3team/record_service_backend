package com.petlog.record.entity;

import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Map;

@Document(collection = "photo_metadata")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PhotoMetadata {

    @Id
    private String id; // MongoDB 내부 ID

    private Long imageId; // PostgreSQL의 DiaryImage imageId와 매핑 (연결고리)

    // 비정형 데이터의 핵심: 규격이 제각각인 데이터를 Map으로 받음
    // 예: EXIF(위도, 경도, 카메라 모델), AI가 분석한 객체(강아지, 고양이 등)
    private Map<String, Object> metadata;

    private String deviceModel; // 촬영 기기 (자주 쓰는 필드는 따로 빼도 됨)
}