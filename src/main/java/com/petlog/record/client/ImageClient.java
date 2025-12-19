package com.petlog.record.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@FeignClient(name = "image-service", url = "${IMAGE_SERVICE_URL}") // 유저 서비스 8080 포트 주소
public interface ImageClient {

    // 유저 서비스의 ImageController 호출
    @PostMapping(value = "/api/images/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    List<String> uploadImageToS3(@RequestPart("multipartFile") List<MultipartFile> multipartFile);

    @PostMapping(value = "/api/archives", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    void createArchive(
            @RequestHeader("X-USER-ID") Long userId,
            @RequestPart("images") List<MultipartFile> images // 컨트롤러의 DTO 필드명과 일치해야 함
    );
}

