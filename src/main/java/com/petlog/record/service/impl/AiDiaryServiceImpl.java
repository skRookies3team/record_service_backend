package com.petlog.record.service.impl;

import com.petlog.record.dto.response.AiDiaryResponse;
import com.petlog.record.entity.Diary;
import com.petlog.record.entity.DiaryImage;
import com.petlog.record.entity.ImageSource;
import com.petlog.record.entity.Visibility;
import com.petlog.record.repository.DiaryRepository;
import com.petlog.record.service.AiDiaryService;
// import com.petlog.record.service.file.S3Uploader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
// [변경 후] 1.0.0-M4 버전용 경로
import org.springframework.ai.model.Media;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AiDiaryServiceImpl implements AiDiaryService {

    private final DiaryRepository diaryRepository;
    private final ChatModel chatModel; // Spring AI의 핵심 인터페이스 (OpenAiChatModel 등이 주입됨)
    // private final S3Uploader s3Uploader;

    @Override
    public Diary createAiDiary(Long userId, Long petId, MultipartFile imageFile, Visibility visibility) {
        log.info("AI Diary creation started for user: {}, pet: {}", userId, petId);

        // 1. [임시] S3 업로드 대체 로직
        // 실제 운영 시: String imageUrl = s3Uploader.upload(imageFile);
        String tempDbUrl = "https://temporary-url/test-image.jpg";

        // 2. Spring AI를 활용한 요청 구성
        // 2-1. 응답 파서 생성 (JSON -> DTO 자동 변환기)
        BeanOutputConverter<AiDiaryResponse> converter = new BeanOutputConverter<>(AiDiaryResponse.class);

        // 2-2. 프롬프트 및 포맷 지시사항 구성
        String instruction = """
                너는 반려동물이야. 사용자가 보낸 사진을 보고 반려동물의 시점에서, 반려동물의 말투로 짧은 일기를 써줘.
                그리고 사진의 분위기를 보고 기분(mood)을 한 단어로 추측해줘.
                """;

        // converter.getFormat()이 "반드시 JSON 포맷으로 응답하라"는 시스템 메시지를 자동으로 생성해줍니다.
        String promptText = instruction + "\n\n" + converter.getFormat();

        try {
            // 2-3. 멀티모달 메시지 생성 (텍스트 + 이미지)
            // Spring AI는 Resource 타입으로 이미지를 받으므로 ByteArrayResource로 변환
            Media imageMedia = new Media(MimeTypeUtils.IMAGE_JPEG, new ByteArrayResource(imageFile.getBytes()));
            UserMessage userMessage = new UserMessage(promptText, List.of(imageMedia));

            // 2-4. AI 모델 호출 및 결과 파싱
            // chatModel.call()로 요청을 보내고, 결과를 converter를 통해 바로 DTO로 변환합니다.
            String responseContent = chatModel.call(new Prompt(userMessage)).getResult().getOutput().getContent();
            AiDiaryResponse aiResponse = converter.convert(responseContent);

            log.debug("AI Response received - Mood: {}", aiResponse.getMood());

            // 3. 엔티티 생성
            Diary diary = Diary.builder()
                    .userId(userId)
                    .petId(petId)
                    .content(aiResponse.getContent())
                    .mood(aiResponse.getMood())
                    .isAiGen(true)
                    .visibility(visibility)
                    .build();

            // 4. 이미지 연관관계 설정
            DiaryImage diaryImage = DiaryImage.builder()
                    .imageUrl(tempDbUrl)
                    .userId(userId)
                    .imgOrder(1)
                    .mainImage(true)
                    .source(ImageSource.GALLERY)
                    .build();

            diary.addImage(diaryImage);

            // 5. DB 저장
            return diaryRepository.save(diary);

        } catch (IOException e) {
            log.error("Image processing failed", e);
            throw new RuntimeException("이미지 처리 중 오류가 발생했습니다.", e);
        }
    }
}