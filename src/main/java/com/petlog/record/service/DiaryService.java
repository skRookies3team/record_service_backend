package com.petlog.record.service;

import com.petlog.record.dto.request.DiaryRequest;
import com.petlog.record.dto.response.AiDiaryResponse;
import com.petlog.record.dto.response.DiaryResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * [다이어리 통합 서비스 인터페이스]
 * 일기의 생명주기(CRUD)를 관리하며, AI를 활용한 미리보기 및
 * 위치/날씨 데이터가 결합된 최종 일기 저장 프로세스를 정의함
 */
public interface DiaryService {


    /**
     * [AI 일기 미리보기 생성]
     * 사용자가 업로드한 사진과 위치, 날짜 정보를 바탕으로 AI가 분석한 초안을 반환
     * 실제 DB 저장 전 사용자가 내용을 확인하고 수정할 수 있도록 제공되는 임시 데이터 프로세스
     * * @param userId 사용자 ID
     * @param petId 반려동물 ID
     * @param images 기존 보관함 선택 이미지 정보
     * @param imageFiles 신규 업로드할 이미지 파일 리스트
     * @param latitude 위도
     * @param longitude 경도
     * @param date 기록 날짜 (문자열 포맷)
     * @return AI 분석 결과 및 이미지 URL이 포함된 미리보기 응답 DTO
     */
    AiDiaryResponse previewAiDiary(Long userId, Long petId, List<DiaryRequest.Image> images, List<MultipartFile> imageFiles, Double latitude, Double longitude, String date);

    /**
     * [최종 일기 저장]
     * AI 미리보기 데이터를 바탕으로 사용자가 수정한 최종 내용을 DB(PostgreSQL, MongoDB)에 영구 저장
     * 저장 성공 시 관련 비동기 이벤트(Kafka, VectorDB) 발행 트리거
     * * @param request 저장할 일기 정보 DTO
     * @return 생성된 다이어리 고유 ID
     */
    Long saveDiary(DiaryRequest.Create request);

    /** [일기 상세 조회] 일기 기본 정보, 이미지 메타데이터 및 스타일 설정을 통합 조회 */
    DiaryResponse getDiary(Long diaryId);

    /** [일기 정보 수정] 일기 본문 및 공개 범위 등 메타데이터 갱신 */
    void updateDiary(Long diaryId, DiaryRequest.Update request);

    /** [일기 삭제] DB 데이터 삭제 및 연관된 외부 리소스 정리 이벤트 전송 */
    void deleteDiary(Long diaryId);
}