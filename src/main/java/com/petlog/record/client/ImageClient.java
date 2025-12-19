package com.petlog.record.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@FeignClient(name = "image-service", url = "${IMAGE_SERVICE_URL}") // 유저 서비스 8080 포트 주소
public interface ImageClient {

    // 유저 서비스의 ImageController 호출
    @PostMapping(value = "/api/images/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    List<String> uploadImageToS3(@RequestPart("multipartFile") List<MultipartFile> multipartFile);
}