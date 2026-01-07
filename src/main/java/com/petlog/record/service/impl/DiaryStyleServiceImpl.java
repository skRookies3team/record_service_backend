package com.petlog.record.service.impl;

import com.petlog.record.dto.request.DiaryStyleRequest;
import com.petlog.record.dto.response.DiaryStyleResponse;
import com.petlog.record.entity.DiaryStyle;
import com.petlog.record.exception.ErrorCode;
import com.petlog.record.exception.ResourceNotFoundException;
import com.petlog.record.exception.UnauthorizedException;
import com.petlog.record.repository.jpa.DiaryStyleRepository;
import com.petlog.record.service.DiaryStyleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * [다이어리 스타일 설정 서비스 구현체]
 * 일기장의 레이아웃, 폰트, 배경색 등 UI 커스터마이징 정보를 관리
 * 유저/펫/일기 단위의 설정 우선순위를 처리하며, 설정 부재 시 시스템 기본값을 제공함
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryStyleServiceImpl implements DiaryStyleService {

    private final DiaryStyleRepository diaryStyleRepository;

    /**
     * [기본 스타일 생성]
     * 특정 사용자나 반려동물에게 부여할 시스템 표준 스타일 설정을 생성하고 저장
     * @return 생성된 기본 DiaryStyle 엔티티
     */
    @Transactional
    public DiaryStyle createDefaultStyle(Long userId, Long petId) {
        DiaryStyle defaultStyle = DiaryStyle.builder()
                .userId(userId)
                .petId(petId)
                .galleryType("grid")
                .textAlignment("left")
                .fontSize(16)
                .sizeOption("medium")
                .backgroundColor("#FFFFFF")
                .preset("default")
                .themeStyle("basic")
                .build();

        return diaryStyleRepository.save(defaultStyle);
    }

    /**
     * [스타일 생성 또는 갱신 (Upsert)]
     * 개별 다이어리에 대한 전용 스타일을 설정하며, 이미 존재하는 경우 기존 설정을 업데이트함
     * @param userId 요청자 ID
     * @param request 스타일 설정 데이터 (diaryId 포함 가능)
     */
    @Override
    @Transactional
    public DiaryStyleResponse createOrUpdateStyle(Long userId, DiaryStyleRequest request) {
        // 1. 개별 다이어리 전용 스타일이 이미 있는지 확인 (우선순위 최고)
        if (request.getDiaryId() != null) {
            Optional<DiaryStyle> existingDiaryStyle = diaryStyleRepository.findByDiaryId(request.getDiaryId());
            if (existingDiaryStyle.isPresent()) {
                return updateStyle(existingDiaryStyle.get().getId(), request, userId);
            }
        }

        // 2. 새로운 스타일 설정 생성 및 저장
        DiaryStyle style = request.toEntity(userId);
        DiaryStyle saved = diaryStyleRepository.save(style);

        return DiaryStyleResponse.fromEntity(saved);
    }

    /**
     * [스타일 정보 수정]
     * 기존 스타일 설정의 각 필드를 부분적으로 수정하며, 소유권 권한 검증을 수행
     * @param styleId 수정 대상 스타일 고유 ID
     * @param userId 수정 요청자 ID (권한 검증용)
     */
    @Override
    @Transactional
    public DiaryStyleResponse updateStyle(Long styleId, DiaryStyleRequest request, Long userId) {
        DiaryStyle style = diaryStyleRepository.findById(styleId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.DIARY_NOT_FOUND, "스타일 설정을 찾을 수 없습니다."));

        // [보안 검증] 자신의 스타일 설정만 수정 가능
        if (!style.getUserId().equals(userId)) {
            throw new UnauthorizedException("해당 스타일 설정을 수정할 권한이 없습니다.");
        }

        // 필드별 null 체크 후 업데이트 (Dirty Checking 활용)
        if (request.getGalleryType() != null) style.setGalleryType(request.getGalleryType());
        if (request.getTextAlignment() != null) style.setTextAlignment(request.getTextAlignment());
        if (request.getFontSize() != null) style.setFontSize(request.getFontSize());
        if (request.getSizeOption() != null) style.setSizeOption(request.getSizeOption());
        if (request.getBackgroundColor() != null) style.setBackgroundColor(request.getBackgroundColor());
        //if (request.getPreset() != null) style.setPreset(request.getPreset());
        if (request.getThemeStyle() != null) style.setThemeStyle(request.getThemeStyle());
        if (request.getFontFamily() != null) style.setFontFamily(request.getFontFamily());

        style.setPreset(request.getPreset() != null ? request.getPreset() : "default");

        return DiaryStyleResponse.fromEntity(style);
    }

    /**
     * [사용자/펫 스타일 조회 및 자동 생성]
     * 특정 펫의 스타일 설정을 조회하며, 데이터가 없는 경우 시스템 기본값으로 자동 생성(Lazy Init)하여 반환
     */
    @Override
    @Transactional(readOnly = true)
    public DiaryStyleResponse getUserStyle(Long userId, Long petId) {
        DiaryStyle style = diaryStyleRepository
                .findByUserIdAndPetId(userId, petId)
                .orElseGet(() -> createDefaultStyle(userId, petId));

        return DiaryStyleResponse.fromEntity(style);
    }

    /**
     * [펫 전용 스타일 조회]
     * getUserStyle을 래핑하여 특정 반려동물의 UI 테마 정보를 조회
     */
    @Override
    @Transactional(readOnly = true)
    public DiaryStyleResponse getPetStyle(Long petId, Long userId) {
        return getUserStyle(userId, petId);
    }

    /**
     * [개별 일기 스타일 조회]
     * 특정 일기(Diary)에 특화되어 설정된 스타일이 있는지 확인
     * @return 스타일 존재 시 DTO 반환, 없을 시 null 반환 (상위 설정 호출 유도)
     */
//    @Override
//    @Transactional(readOnly = true)
//    public DiaryStyleResponse getDiaryStyle(Long diaryId) {
//        DiaryStyle style = diaryStyleRepository.findByDiaryId(diaryId)
//                .orElse(null);
//
//        return style != null ? DiaryStyleResponse.fromEntity(style) : null;
//    }

    @Transactional(readOnly = true)
    public DiaryStyleResponse getDiaryStyle(Long diaryId) {
        DiaryStyle style = diaryStyleRepository.findByDiaryId(diaryId)
                .orElse(null); // 없으면 null 반환하거나 예외 처리

        if (style == null) return null;
        return DiaryStyleResponse.fromEntity(style); // DTO 변환
    }
}