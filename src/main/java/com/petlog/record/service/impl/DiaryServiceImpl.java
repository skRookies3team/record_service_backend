package com.petlog.record.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlog.record.client.ImageClient;
import com.petlog.record.client.PetClient;
import com.petlog.record.client.UserClient;
import com.petlog.record.dto.request.DiaryRequest;
import com.petlog.record.dto.response.AiDiaryResponse;
import com.petlog.record.dto.client.ArchiveResponse;
import com.petlog.record.dto.response.DiaryResponse;
import com.petlog.record.entity.*;
import com.petlog.record.exception.BusinessException;
import com.petlog.record.exception.EntityNotFoundException;
import com.petlog.record.exception.ErrorCode;
import com.petlog.record.repository.DiaryArchiveRepository;
import com.petlog.record.repository.DiaryRepository;
import com.petlog.record.service.DiaryService;
import com.petlog.record.service.WeatherService;
import com.petlog.record.util.LatXLngY;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.model.Media;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryServiceImpl implements DiaryService {

    private final DiaryRepository diaryRepository;
    private final DiaryArchiveRepository diaryArchiveRepository;
    private final UserClient userClient;
    private final PetClient petClient;
    private final ImageClient imageClient;
    private final ChatModel chatModel;
    private final WeatherService weatherService;
    private final RestTemplate restTemplate;

    @Value("classpath:prompts/diary-system.st")
    private Resource systemPromptResource;

    @Value("${kakao.rest-api-key}")
    private String kakaoRestApiKey;

    @Override
    @Transactional
    public AiDiaryResponse previewAiDiary(Long userId, Long petId, List<DiaryRequest.Image> images, List<MultipartFile> imageFiles) {
        log.info("AI Diary Preview started. User: {}, Pet: {}", userId, petId);
        validateUserAndPet(userId, petId);

        List<String> finalImageUrls = new ArrayList<>();
        List<Long> finalArchiveIds = new ArrayList<>();

        // 1. 이미지 처리 (보관함 선택 + 신규 업로드)
        if (images != null) {
            for (DiaryRequest.Image img : images) {
                if (ImageSource.ARCHIVE.equals(img.getSource()) && img.getArchiveId() != null) {
                    finalImageUrls.add(img.getImageUrl());
                    finalArchiveIds.add(img.getArchiveId());
                }
            }
        }

        if (isActualFilePresent(imageFiles)) {
            try {
                ArchiveResponse.CreateArchiveDtoList archiveResponse = imageClient.createArchive(userId, imageFiles);
                for (ArchiveResponse.CreateArchiveDto dto : archiveResponse.getArchives()) {
                    finalImageUrls.add(dto.getUrl());
                    finalArchiveIds.add(dto.getArchiveId());
                }
            } catch (Exception e) {
                log.error("Image Server Upload Failed", e);
                throw new RuntimeException("이미지 서버 연동 실패");
            }
        }

        if (finalImageUrls.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        }

        // 2. AI 분석 실행
        AiDiaryResponse aiResponse = generateContentWithAiFromUrls(finalImageUrls);

        // 3. 응답 객체에 이미지 정보 주입 (AiDiaryResponse에 @Setter가 있어야 함)
        aiResponse.setImageUrls(finalImageUrls);
        aiResponse.setArchiveIds(finalArchiveIds);

        return aiResponse;
    }

    @Override
    @Transactional
    public Long saveDiary(DiaryRequest.Create request) {
        log.info("Final Diary Saving. User: {}", request.getUserId());

        // 1. 날씨 및 주소 정보 보정
        String weatherInfo = request.getWeather();
        if ((weatherInfo == null || weatherInfo.isEmpty()) && request.getLatitude() != null && request.getLongitude() != null) {
            try {
                int[] grid = LatXLngY.convert(request.getLatitude(), request.getLongitude());
                weatherInfo = weatherService.getCurrentWeather(grid[0], grid[1]);
            } catch (Exception e) { weatherInfo = "맑음"; }
        }

        String finalLocationName = request.getLocationName();
        if ((finalLocationName == null || finalLocationName.isEmpty()) && request.getLatitude() != null && request.getLongitude() != null) {
            finalLocationName = getAddressFromCoords(request.getLatitude(), request.getLongitude());
        }

        // 2. Diary 엔티티 생성
        Diary diary = Diary.builder()
                .userId(request.getUserId())
                .petId(request.getPetId())
                .content(request.getContent())
                .mood(request.getMood())
                .weather(weatherInfo)
                .isAiGen(request.getIsAiGen())
                .visibility(request.getVisibility() != null ? request.getVisibility() : Visibility.PRIVATE)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .locationName(finalLocationName)
                .date(request.getDate() != null ? request.getDate() : LocalDate.now())
                .build();

        // 3. 이미지 정보 연결
        if (request.getImageUrls() != null) {
            for (int i = 0; i < request.getImageUrls().size(); i++) {
                DiaryImage diaryImage = DiaryImage.builder()
                        .imageUrl(request.getImageUrls().get(i))
                        .userId(request.getUserId())
                        .imgOrder(i + 1)
                        .mainImage(i == 0)
                        .source(ImageSource.ARCHIVE)
                        .build();
                diary.addImage(diaryImage);
            }
        }

        Diary savedDiary = diaryRepository.save(diary);

        // 4. Diary-Archive 매핑 저장
        if (request.getArchiveIds() != null) {
            for (Long archiveId : request.getArchiveIds()) {
                diaryArchiveRepository.save(DiaryArchive.create(savedDiary, archiveId));
            }
        }

        return savedDiary.getDiaryId();
    }

    // AI 분석 헬퍼 메서드 (URL 기반)
    private AiDiaryResponse generateContentWithAiFromUrls(List<String> imageUrls) {
        BeanOutputConverter<AiDiaryResponse> converter = new BeanOutputConverter<>(AiDiaryResponse.class);
        String baseSystemPrompt = new PromptTemplate(systemPromptResource).render();
        SystemMessage systemMessage = new SystemMessage(baseSystemPrompt + "\n\n보관함의 사진들을 분석하여 일기를 작성하세요.");

        try {
            List<Media> mediaList = new ArrayList<>();
            for (String url : imageUrls) {
                mediaList.add(new Media(MimeTypeUtils.IMAGE_JPEG, new URL(url)));
            }
            UserMessage userMessage = new UserMessage("분석하여 JSON 형식으로 응답하세요.\n" + converter.getFormat(), mediaList);
            Prompt prompt = new Prompt(List.of(systemMessage, userMessage), OpenAiChatOptions.builder().withModel("gpt-4o").build());
            return converter.convert(chatModel.call(prompt).getResult().getOutput().getContent());
        } catch (Exception e) {
            log.error("AI 분석 실패", e);
            throw new RuntimeException("AI 일기 생성 실패");
        }
    }

    @Override
    public DiaryResponse getDiary(Long diaryId) {
        Diary diary = diaryRepository.findById(diaryId).orElseThrow(() -> new EntityNotFoundException(ErrorCode.DIARY_NOT_FOUND));
        return DiaryResponse.fromEntity(diary);
    }

    @Override
    @Transactional
    public void updateDiary(Long diaryId, DiaryRequest.Update request) {
        Diary diary = diaryRepository.findById(diaryId).orElseThrow(() -> new EntityNotFoundException(ErrorCode.DIARY_NOT_FOUND));
        diary.update(request.getContent(), request.getVisibility(), request.getWeather(), request.getMood());
    }

    @Override
    @Transactional
    public void deleteDiary(Long diaryId) {
        Diary diary = diaryRepository.findById(diaryId).orElseThrow(() -> new EntityNotFoundException(ErrorCode.DIARY_NOT_FOUND));
        diaryRepository.delete(diary);
    }

    private void validateUserAndPet(Long userId, Long petId) {
        try { userClient.getUserInfo(userId); } catch (Exception e) { throw new EntityNotFoundException(ErrorCode.USER_NOT_FOUND); }
        try { petClient.getPetInfo(petId); } catch (FeignException e) { throw new EntityNotFoundException(ErrorCode.PET_NOT_FOUND); }
    }

    private boolean isActualFilePresent(List<MultipartFile> files) {
        return files != null && !files.isEmpty() && !files.get(0).isEmpty();
    }

    private String getAddressFromCoords(Double lat, Double lng) {
        try {
            String url = String.format("https://dapi.kakao.com/v2/local/geo/coord2regioncode.json?x=%s&y=%s", lng, lat);
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + kakaoRestApiKey);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());
            JsonNode documents = root.path("documents");
            if (documents.isArray() && documents.size() > 0) {
                for (JsonNode doc : documents) {
                    if ("H".equals(doc.path("region_type").asText())) return doc.path("address_name").asText();
                }
            }
        } catch (Exception e) { log.error("주소 변환 실패"); }
        return null;
    }
}