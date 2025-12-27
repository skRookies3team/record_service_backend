package com.petlog.record.client;

import com.petlog.record.dto.client.ArchiveResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@FeignClient(name = "image-service", url = "${IMAGE_SERVICE_URL}") // 유저 서비스 8080 포트 주소
public interface ImageClient {

    /**
     * [사진 생성 API 호출]
     * 유저 서비스의 ArchiveController.createArchive를 호출합니다.
     * 이 API는 내부적으로 S3 업로드와 보관함 저장을 수행하고 생성된 URL 리스트를 반환합니다.
     */
    @PostMapping(value = "/api/archives", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ArchiveResponse.CreateArchiveDtoList createArchive(
            @RequestHeader("X-USER-ID") Long userId,
            @RequestPart("images") List<MultipartFile> images
    );


    @GetMapping("/api/archives/{archiveId}/url")
    String getArchiveUrl(@PathVariable("archiveId") Long archiveId);
}