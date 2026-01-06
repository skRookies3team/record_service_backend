package com.petlog.record.client;

import com.petlog.record.dto.client.ArchiveResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * [이미지 서비스 외부 API 호출]
 * 이미지 서비스(image-service)와 통신하여 사진 업로드 및 URL 조회를 처리하기 위한 FeignClient
 */
@FeignClient(name = "image-service", url = "${IMAGE_SERVICE_URL}")
public interface ImageClient {

    /**
     * [사진 생성 API 호출]
     * 유저 서비스의 ArchiveController.createArchive를 호출
     * 이 API는 내부적으로 S3 업로드와 보관함 저장을 수행하고 생성된 URL 리스트를 반환
     */
    @PostMapping(value = "/api/archives", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ArchiveResponse.CreateArchiveDtoList createArchive(
            @RequestHeader("X-USER-ID") Long userId,
            @RequestPart("images") List<MultipartFile> images
    );

    /**
     * [보관함 URL 조회 API 호출]
     * 이미지 서비스의 ArchiveController.getArchiveUrl을 호출
     * 보관함 ID를 통해 저장된 이미지의 접근 가능한 URL을 조회
     */
    @GetMapping("/api/archives/{archiveId}/url")
    String getArchiveUrl(@PathVariable("archiveId") Long archiveId);
}