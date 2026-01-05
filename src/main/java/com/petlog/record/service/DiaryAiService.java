package com.petlog.record.service;

import com.petlog.record.dto.response.AiDiaryResponse;
import java.util.List;

public interface DiaryAiService {
    AiDiaryResponse generateContentWithAiFromUrls(List<String> imageUrls);
}