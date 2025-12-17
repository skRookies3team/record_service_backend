package com.petlog.record.service.impl;

import com.petlog.record.client.PetServiceClient;
import com.petlog.record.client.StorageServiceClient;
import com.petlog.record.client.UserServiceClient;
import com.petlog.record.dto.request.DiaryRequest;
import com.petlog.record.dto.response.AiDiaryResponse;
import com.petlog.record.dto.response.DiaryResponse;
import com.petlog.record.entity.Diary;
import com.petlog.record.entity.DiaryImage;
import com.petlog.record.entity.ImageSource;
import com.petlog.record.entity.Visibility;
import com.petlog.record.exception.EntityNotFoundException;
import com.petlog.record.exception.ErrorCode;
import com.petlog.record.repository.DiaryRepository;
import com.petlog.record.service.DiaryService;
import com.petlog.record.service.WeatherService;
import com.petlog.record.util.LatXLngY;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.model.Media;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryServiceImpl implements DiaryService {

    private final DiaryRepository diaryRepository;
    private final UserServiceClient userClient;
    private final PetServiceClient petClient;

    @Qualifier("mockStorageServiceClient")
    private final StorageServiceClient storageServiceClient;

    private final ChatModel chatModel;
    private final WeatherService weatherService;

    @Value("classpath:prompts/diary-system.st")
    private Resource systemPromptResource;

    @Override
    @Transactional
    public Long createAiDiary(Long userId, Long petId, MultipartFile imageFile, Visibility visibility, Double latitude, Double longitude, LocalDate date) {
        log.info("AI Diary creation started for user: {}, pet: {}, date: {}", userId, petId, date);

        validateUserAndPet(userId, petId);

        String uploadedImageUrl = "https://temporary-url/test-image.jpg";

        // 1. AI 일기 내용 생성
        AiDiaryResponse aiResponse = generateContentWithAi(imageFile);

        // 2. 날씨 정보 처리 (오늘/과거 분기 처리)
        String weatherInfo = "맑음"; // 기본값
        LocalDate targetDate = date != null ? date : LocalDate.now();

        try {
            if (latitude != null && longitude != null) {
                // (1) 오늘 날짜인 경우: 초단기예보 (격자 좌표 변환 필요)
                if (targetDate.isEqual(LocalDate.now())) {
                    int[] grid = LatXLngY.convert(latitude, longitude);
                    weatherInfo = weatherService.getCurrentWeather(grid[0], grid[1]);
                    log.info("TODAY Weather API Result: {}", weatherInfo);
                }
                // (2) 과거 날짜인 경우: ASOS 일자료 (관측소 매핑 필요)
                else if (targetDate.isBefore(LocalDate.now())) {
                    weatherInfo = weatherService.getPastWeather(targetDate, latitude, longitude);
                    log.info("PAST Weather API Result ({}): {}", targetDate, weatherInfo);
                }
            } else {
                log.info("Location info not provided. Skipping weather API.");
            }
        } catch (Exception e) {
            log.warn("Weather API call failed, using default: {}", e.getMessage());
        }

        // 3. 일기 저장
        Diary diary = Diary.builder()
                .userId(userId)
                .petId(petId)
                .content(aiResponse.getContent())
                .mood(aiResponse.getMood())
                .weather(weatherInfo) // API 결과 적용
                .isAiGen(true)
                .visibility(visibility)
                // 만약 Diary 엔티티에 기록 날짜 필드(recordDate)가 있다면 여기서 설정
                // .recordDate(targetDate)
                .build();

        DiaryImage diaryImage = DiaryImage.builder()
                .imageUrl(uploadedImageUrl)
                .userId(userId)
                .imgOrder(1)
                .mainImage(true)
                .source(ImageSource.GALLERY)
                .build();

        diary.addImage(diaryImage);
        Diary savedDiary = diaryRepository.save(diary);

        processDiaryImages(savedDiary);

        return savedDiary.getDiaryId();
    }

    // ... (이하 Private Methods 및 CRUD 메서드는 기존과 동일) ...
    private AiDiaryResponse generateContentWithAi(MultipartFile imageFile) {
        BeanOutputConverter<AiDiaryResponse> converter = new BeanOutputConverter<>(AiDiaryResponse.class);
        String systemPromptText = new PromptTemplate(systemPromptResource).render();
        String promptText = systemPromptText + "\n\n" + converter.getFormat();

        try {
            Media imageMedia = new Media(MimeTypeUtils.IMAGE_JPEG, new ByteArrayResource(imageFile.getBytes()));
            UserMessage userMessage = new UserMessage(promptText, List.of(imageMedia));

            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .withTemperature(0.7)
                    .withModel("gpt-4o")
                    .build();

            Prompt prompt = new Prompt(userMessage, options);
            String responseContent = chatModel.call(prompt).getResult().getOutput().getContent();

            return converter.convert(responseContent);
        } catch (IOException e) {
            log.error("Image processing failed", e);
            throw new RuntimeException("AI 생성 실패", e);
        }
    }

    private void validateUserAndPet(Long userId, Long petId) {
        try {
            userClient.getUserInfo(userId);
        } catch (FeignException e) {
            throw new EntityNotFoundException(ErrorCode.USER_NOT_FOUND);
        }
        try {
            petClient.getPetInfo(petId);
        } catch (FeignException e) {
            throw new EntityNotFoundException(ErrorCode.PET_NOT_FOUND);
        }
    }

    private void processDiaryImages(Diary diary) {
        List<DiaryImage> images = diary.getImages();
        if (images == null || images.isEmpty()) return;

        List<StorageServiceClient.PhotoRequest> newPhotos = images.stream()
                .filter(img -> img.getSource() == ImageSource.GALLERY)
                .map(img -> new StorageServiceClient.PhotoRequest(
                        img.getUserId(),
                        img.getImageUrl()
                ))
                .collect(Collectors.toList());

        if (!newPhotos.isEmpty()) {
            try {
                storageServiceClient.savePhotos(newPhotos);
            } catch (Exception e) {
                log.warn("Storage Service Transfer Failed: {}", e.getMessage());
            }
        }
    }

//    @Override
//    @Transactional
//    public Long createDiary(DiaryRequest.Create request) {
//        validateUserAndPet(request.getUserId(), request.getPetId());
//        Diary diary = request.toEntity();
//        Diary savedDiary = diaryRepository.save(diary);
//        processDiaryImages(diary);
//        return savedDiary.getDiaryId();
//    }

    @Override
    public DiaryResponse getDiary(Long diaryId) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.DIARY_NOT_FOUND));
        return DiaryResponse.fromEntity(diary);
    }

    @Override
    @Transactional
    public void updateDiary(Long diaryId, DiaryRequest.Update request) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.DIARY_NOT_FOUND));

        diary.update(
                request.getContent() != null ? request.getContent() : diary.getContent(),
                request.getVisibility() != null ? request.getVisibility() : diary.getVisibility(),
                request.getWeather() != null ? request.getWeather() : diary.getWeather(),
                request.getMood() != null ? request.getMood() : diary.getMood()
        );
    }

    @Override
    @Transactional
    public void deleteDiary(Long diaryId) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.DIARY_NOT_FOUND));
        diaryRepository.delete(diary);
    }
}