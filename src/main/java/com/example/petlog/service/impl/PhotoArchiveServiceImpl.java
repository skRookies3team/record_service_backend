package com.example.petlog.service.impl;

import com.example.petlog.dto.response.PhotoArchiveResponse;
import com.example.petlog.entity.PhotoArchive;
import com.example.petlog.repository.PhotoArchiveRepository;
import com.example.petlog.service.PhotoArchiveService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PhotoArchiveServiceImpl implements PhotoArchiveService {

    private final PhotoArchiveRepository photoArchiveRepository;

    @Override
    @Transactional
    public void saveArchives(List<PhotoArchive> archives) {
        if (archives != null && !archives.isEmpty()) {
            photoArchiveRepository.saveAll(archives);
        }
    }

    @Override
    public List<PhotoArchiveResponse> getPhotoArchives(Long userId) {
        return photoArchiveRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(PhotoArchiveResponse::fromEntity)
                .collect(Collectors.toList());
    }
}