package com.petlog.record.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlog.record.client.ImageClient;
import com.petlog.record.client.PetClient;
import com.petlog.record.client.UserClient;
import com.petlog.record.dto.request.DiaryRequest;
import com.petlog.record.dto.response.AiDiaryResponse;
import com.petlog.record.dto.response.DiaryResponse;
import com.petlog.record.entity.Diary;
import com.petlog.record.entity.DiaryImage;
import com.petlog.record.entity.ImageSource;
import com.petlog.record.entity.Visibility;
import com.petlog.record.exception.BusinessException;
import com.petlog.record.exception.EntityNotFoundException;
import com.petlog.record.exception.ErrorCode;
import com.petlog.record.infrastructure.kafka.DiaryEventProducer;
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
import org.springframework.ai.document.Document;
import org.springframework.ai.model.Media;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryServiceImpl implements DiaryService {

    private final DiaryRepository diaryRepository;
    private final UserClient userClient;
    private final PetClient petClient;
    private final ImageClient imageClient;
    private final ChatModel chatModel;
    private final WeatherService weatherService;
    private final RestTemplate restTemplate;

    // 인프라 스트럭처 주입
    private final VectorStore vectorStore;                // Milvus 저장용
    private final DiaryEventProducer diaryEventProducer;  // Kafka 이벤트 발행용

    @Value("classpath:prompts/diary-system.st")
    private Resource systemPromptResource;

    @Value("${kakao.rest-api-key}")
    private String kakaoRestApiKey;

    /**
     * [STEP 1] AI 일기 생성 및 DB 임시 저장
     * 이미지를 업로드하고 AI 일기 초안을 작성하여 PostgreSQL에만 저장합니다.
     */
    @Override
    @Transactional
    public Long createAiDiary(Long userId, Long petId, Long photoArchiveId, List<MultipartFile> imageFiles,
                              Visibility visibility, String locationName,
                              Double latitude, Double longitude, LocalDate date) {

        log.info("AI Diary Step 1: Generating draft for user: {}", userId);

        validateUserAndPet(userId, petId);

        // 1. S3 이미지 업로드
        List<String> uploadedImageUrls = imageClient.uploadImageToS3(imageFiles);

        // 2. AI 멀티 이미지 분석 및 내용 생성
        AiDiaryResponse aiResponse = generateContentWithAi(imageFiles);

        // 3. 날씨 정보 처리
        String weatherInfo = fetchWeatherInfo(latitude, longitude);

        // 4. 주소 변환
        String finalLocationName = (locationName == null || locationName.isEmpty())
                ? getAddressFromCoords(latitude, longitude) : locationName;

        // 5. Diary 엔티티 생성 및 저장
        Diary diary = Diary.builder()
                .userId(userId).petId(petId).photoArchiveId(photoArchiveId)
                .content(aiResponse.getContent()).mood(aiResponse.getMood())
                .weather(weatherInfo).isAiGen(true)
                .visibility(visibility != null ? visibility : Visibility.PRIVATE)
                .latitude(latitude).longitude(longitude)
                .locationName(finalLocationName != null ? finalLocationName : "위치 정보 없음")
                .date(date != null ? date : LocalDate.now())
                .build();

        for (int i = 0; i < uploadedImageUrls.size(); i++) {
            diary.addImage(DiaryImage.builder()
                    .imageUrl(uploadedImageUrls.get(i))
                    .userId(userId).imgOrder(i + 1)
                    .mainImage(i == 0).source(ImageSource.GALLERY).build());
        }

        Diary savedDiary = diaryRepository.save(diary);

        // 보관함 자동 저장 요청
        autoArchiveDiaryImages(savedDiary.getUserId(), imageFiles);

        return savedDiary.getDiaryId();
    }

    /**
     * [STEP 2] 사용자 일기 확정 및 외부 시스템 동기화
     * 사용자가 '저장' 버튼을 눌렀을 때 Milvus에 저장하고 Kafka 이벤트를 발행합니다.
     */
    @Override
    @Transactional
    public DiaryResponse confirmAndPublishDiary(Long diaryId) {
        log.info("AI Diary Step 2: Publishing confirmed diaryId: {}", diaryId);

        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.DIARY_NOT_FOUND));

        // 1. Milvus 벡터 DB 저장 (검색용)
        saveDiaryToVectorDB(diary);

        // 2. Kafka 이벤트 발행 (Healthcare Service 전송)
        try {
            String firstImageUrl = diary.getImages().isEmpty() ? null : diary.getImages().get(0).getImageUrl();

            diaryEventProducer.publishDiaryCreatedEvent(
                    diary.getDiaryId(),
                    diary.getUserId(),
                    diary.getPetId(),
                    diary.getContent(),
                    firstImageUrl
            );
            log.info("✅ Kafka event published for diaryId: {}", diaryId);
        } catch (Exception e) {
            log.error("❌ Kafka publishing failed, but continuing: {}", e.getMessage());
        }

        return DiaryResponse.fromEntity(diary);
    }

    /**
     * Milvus Vector DB 저장 로직
     */
    private void saveDiaryToVectorDB(Diary diary) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("userId", diary.getUserId());
            metadata.put("diaryId", diary.getDiaryId());
            metadata.put("date", diary.getDate().toString());

            Document document = new Document(diary.getContent(), metadata);
            vectorStore.add(List.of(document));
            log.info("✅ Successfully saved to Milvus");
        } catch (Exception e) {
            log.error("❌ Milvus save failed: {}", e.getMessage());
        }
    }

    /**
     * AI 멀티 이미지 분석 로직
     */
    private AiDiaryResponse generateContentWithAi(List<MultipartFile> imageFiles) {
        BeanOutputConverter<AiDiaryResponse> converter = new BeanOutputConverter<>(AiDiaryResponse.class);
        String basePrompt = new PromptTemplate(systemPromptResource).render();
        String instructions = String.format("\n\n이미지 %d장을 분석하여 하나의 스토리로 JSON 응답하세요.", imageFiles.size());

        SystemMessage systemMessage = new SystemMessage(basePrompt + instructions);

        try {
            List<Media> mediaList = new ArrayList<>();
            for (MultipartFile file : imageFiles) {
                mediaList.add(new Media(MimeTypeUtils.IMAGE_JPEG, new ByteArrayResource(file.getBytes())));
            }

            UserMessage userMessage = new UserMessage("분석 결과 JSON 형식: " + converter.getFormat(), mediaList);
            Prompt prompt = new Prompt(List.of(systemMessage, userMessage),
                    OpenAiChatOptions.builder().withModel("gpt-4o").withTemperature(0.6).build());

            return converter.convert(chatModel.call(prompt).getResult().getOutput().getContent());
        } catch (Exception e) {
            throw new RuntimeException("AI 분석 실패: " + e.getMessage());
        }
    }

    // --- 기타 헬퍼 메서드 및 비즈니스 로직 (Update/Delete 등) ---

    private String fetchWeatherInfo(Double lat, Double lng) {
        if (lat == null || lng == null) return "맑음";
        try {
            int[] grid = LatXLngY.convert(lat, lng);
            return weatherService.getCurrentWeather(grid[0], grid[1]);
        } catch (Exception e) { return "맑음"; }
    }

    private void validateUserAndPet(Long userId, Long petId) {
        try { userClient.getUserInfo(userId); } catch (Exception e) { throw new EntityNotFoundException(ErrorCode.USER_NOT_FOUND); }
        try { petClient.getPetInfo(petId); } catch (FeignException e) { throw new EntityNotFoundException(ErrorCode.PET_NOT_FOUND); }
    }

    private String getAddressFromCoords(Double lat, Double lng) {
        try {
            String url = String.format("https://dapi.kakao.com/v2/local/geo/coord2regioncode.json?x=%s&y=%s", lng, lat);
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + kakaoRestApiKey);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            JsonNode root = new ObjectMapper().readTree(response.getBody());
            return root.path("documents").get(0).path("address_name").asText();
        } catch (Exception e) { return "위치 정보 없음"; }
    }

    private void autoArchiveDiaryImages(Long userId, List<MultipartFile> imageFiles) {
        try { imageClient.createArchive(userId, imageFiles); } catch (Exception e) { log.warn("보관함 연동 실패"); }
    }

    @Override
    @Transactional
    public void updateDiary(Long diaryId, DiaryRequest.Update request) {
        Diary diary = diaryRepository.findById(diaryId).orElseThrow(() -> new EntityNotFoundException(ErrorCode.DIARY_NOT_FOUND));
        diary.update(request.getContent(), request.getVisibility(), request.getWeather(), request.getMood());
        diaryEventProducer.publishDiaryUpdatedEvent(diary.getDiaryId(), diary.getUserId(), diary.getPetId(), diary.getContent());
    }

    @Override
    @Transactional
    public void deleteDiary(Long diaryId) {
        Diary diary = diaryRepository.findById(diaryId).orElseThrow(() -> new EntityNotFoundException(ErrorCode.DIARY_NOT_FOUND));
        Long uId = diary.getUserId();
        Long pId = diary.getPetId();
        diaryRepository.delete(diary);
        diaryEventProducer.publishDiaryDeletedEvent(diaryId, uId, pId);
    }

    @Override
    public DiaryResponse getDiary(Long diaryId) {
        return DiaryResponse.fromEntity(diaryRepository.findById(diaryId).orElseThrow(() -> new EntityNotFoundException(ErrorCode.DIARY_NOT_FOUND)));
    }
}