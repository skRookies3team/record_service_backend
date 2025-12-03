package com.example.petlog.controller;

import com.example.petlog.dto.response.PhotoArchiveResponse;
import com.example.petlog.service.PhotoArchiveService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/archives")
@RequiredArgsConstructor
public class PhotoArchiveController {

    private final PhotoArchiveService photoArchiveService;

    // 사용자별 사진 보관함 목록 조회
    // GET /api/archives/user/{userId}
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PhotoArchiveResponse>> getPhotoArchives(@PathVariable Long userId) {
        return ResponseEntity.ok(photoArchiveService.getPhotoArchives(userId));
    }
}