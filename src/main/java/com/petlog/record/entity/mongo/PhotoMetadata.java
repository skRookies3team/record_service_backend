package com.petlog.record.entity.mongo;

import org.springframework.data.annotation.Id;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Map;

/**
 * [사진 상세 메타데이터 엔티티 - MongoDB]
 * RDB(PostgreSQL)에 담기 어려운 대용량/비정형 사진 정보를 저장하는 NoSQL 문서 객체
 * 사진의 EXIF 정보나 AI 사물 인식 태그 등 규격이 제각각인 데이터를 유연하게 관리
 */
@Document(collection = "photo_metadata")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PhotoMetadata {

    @Id
    private String id; // MongoDB 내부 ID


    /** * [RDB 연결고리]
     * PostgreSQL의 DiaryImage 엔티티의 imageId와 1:1로 매핑되는 식별자
     * 이 필드를 통해 분산된 두 DB 간의 데이터 일관성을 유지함
     */
    private Long imageId;

    /**
     * [비정형 상세 데이터]
     * - EXIF: 위도, 경도, 카메라 모델, 렌즈 정보 등
     * - AI 분석: 탐지된 객체(강아지, 장난감 등), 색상 정보, 추천 태그 등
     */
    private Map<String, Object> metadata;

    /** 촬영 기기 모델 명칭 (필터링 및 통계용 별도 추출 필드) */
    private String deviceModel;

}