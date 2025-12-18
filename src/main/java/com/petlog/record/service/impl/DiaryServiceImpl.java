package com.petlog.record.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.client.RestTemplate;
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
    private final RestTemplate restTemplate;

    @Value("classpath:prompts/diary-system.st")
    private Resource systemPromptResource;

    @Value("${kakao.rest-api-key}")
    private String kakaoRestApiKey;

    @Override
    @Transactional
    public Long createAiDiary(Long userId, Long petId, MultipartFile imageFile,
                              Visibility visibility, String locationName,
                              Double latitude, Double longitude, LocalDate date) {
        log.info("AI Diary creation started for user: {}, pet: {}", userId, petId);

        validateUserAndPet(userId, petId);

        // TODO: 실제 S3 업로드 로직으로 교체 필요
        String uploadedImageUrl = "https://temporary-url/test-image.jpg";

        // 1. AI 일기 내용 생성
        AiDiaryResponse aiResponse = generateContentWithAi(imageFile);

        // 2. 날씨 정보 처리
        String weatherInfo = "맑음";
        LocalDate targetDate = date != null ? date : LocalDate.now();

        try {
            if (latitude != null && longitude != null && latitude != 0 && longitude != 0) {
                if (targetDate.isEqual(LocalDate.now())) {
                    int[] grid = LatXLngY.convert(latitude, longitude);
                    weatherInfo = weatherService.getCurrentWeather(grid[0], grid[1]);
                } else if (targetDate.isBefore(LocalDate.now())) {
                    weatherInfo = weatherService.getPastWeather(targetDate, latitude, longitude);
                }
            }
        } catch (Exception e) {
            log.warn("Weather API call failed: {}", e.getMessage());
        }

        // 3. 주소 변환 (프론트에서 locationName을 안 보내므로 백엔드 변환 필수)
        // locationName이 비어있으면 좌표를 사용하여 변환 시도
        String finalLocationName = locationName;
        if ((finalLocationName == null || finalLocationName.isEmpty()) && latitude != null && longitude != null && latitude != 0 && longitude != 0) {
            log.info("좌표 기반 주소 변환 시도: lat={}, lng={}", latitude, longitude);
            finalLocationName = getAddressFromCoords(latitude, longitude);
            log.info("주소 변환 결과: {}", finalLocationName);
        } else if (finalLocationName == null) {
            finalLocationName = "위치 정보 없음";
        }

        // 4. 일기 저장
        Diary diary = Diary.builder()
                .userId(userId)
                .petId(petId)
                .content(aiResponse.getContent())
                .mood(aiResponse.getMood())
                .weather(weatherInfo)
                .isAiGen(true)
                .visibility(visibility)
                .latitude(latitude)
                .longitude(longitude)
                .locationName(finalLocationName) // 변환된 주소 저장
                .date(targetDate)
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

    // [Private Methods]

    private AiDiaryResponse generateContentWithAi(MultipartFile imageFile) {
        BeanOutputConverter<AiDiaryResponse> converter = new BeanOutputConverter<>(AiDiaryResponse.class);
        String systemPromptText = new PromptTemplate(systemPromptResource).render();
        String promptText = systemPromptText + "\n\n" + converter.getFormat();

        try {
            Media imageMedia = new Media(MimeTypeUtils.IMAGE_JPEG, new ByteArrayResource(imageFile.getBytes()));
            UserMessage userMessage = new UserMessage(promptText, List.of(imageMedia));
            OpenAiChatOptions options = OpenAiChatOptions.builder().withTemperature(0.7).withModel("gpt-4o").build();
            Prompt prompt = new Prompt(userMessage, options);
            String responseContent = chatModel.call(prompt).getResult().getOutput().getContent();
            return converter.convert(responseContent);
        } catch (IOException e) {
            log.error("Image processing failed", e);
            throw new RuntimeException("AI 생성 실패", e);
        }
    }

    private void validateUserAndPet(Long userId, Long petId) {
        try { userClient.getUserInfo(userId); } catch (FeignException e) { throw new EntityNotFoundException(ErrorCode.USER_NOT_FOUND); }
        try { petClient.getPetInfo(petId); } catch (FeignException e) { throw new EntityNotFoundException(ErrorCode.PET_NOT_FOUND); }
    }

    private void processDiaryImages(Diary diary) {
        List<DiaryImage> images = diary.getImages();
        if (images == null || images.isEmpty()) return;

        List<StorageServiceClient.PhotoRequest> newPhotos = images.stream()
                .filter(img -> img.getSource() == ImageSource.GALLERY)
                .map(img -> new StorageServiceClient.PhotoRequest(img.getUserId(), img.getImageUrl()))
                .collect(Collectors.toList());

        if (!newPhotos.isEmpty()) {
            try { storageServiceClient.savePhotos(newPhotos); }
            catch (Exception e) { log.warn("Storage Service Transfer Failed: {}", e.getMessage()); }
        }
    }

    // [중요] 카카오 API 디버깅 로그 추가 및 로직 보강
    private String getAddressFromCoords(Double lat, Double lng) {
        try {
            // API Key 확인 (로그에는 앞 5자리만 노출)
            if (kakaoRestApiKey == null || kakaoRestApiKey.isEmpty()) {
                log.error("Kakao API Key가 설정되지 않았습니다.");
                return null;
            }
            String maskedKey = kakaoRestApiKey.length() > 5 ? kakaoRestApiKey.substring(0, 5) + "..." : "SHORT_KEY";
            log.info("Kakao API Key Loaded: {}", maskedKey);

            // x=경도(lng), y=위도(lat) 순서 확인
            String url = String.format("https://dapi.kakao.com/v2/local/geo/coord2regioncode.json?x=%s&y=%s", lng, lat);
            log.info("Kakao API URL: {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + kakaoRestApiKey);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            // 응답 로그 확인 (성공 시 JSON 전체 출력)
            log.debug("Kakao API Response: {}", response.getBody());

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());
            JsonNode documents = root.path("documents");

            if (documents.isArray() && documents.size() > 0) {
                // 1. 행정동(H) 찾기 (가장 정확한 동 단위)
                for (JsonNode doc : documents) {
                    if ("H".equals(doc.path("region_type").asText())) {
                        String address = doc.path("address_name").asText();
                        log.info("Found Region(H): {}", address);
                        return address;
                    }
                }
                // 2. 행정동 없으면 법정동(B) 반환
                String address = documents.get(0).path("address_name").asText();
                log.info("Found Region(B - Fallback): {}", address);
                return address;
            } else {
                log.warn("Kakao API 응답에 documents가 비어있습니다. 좌표가 해상이나 지원되지 않는 지역일 수 있습니다.");
            }
        } catch (Exception e) {
            log.error("Kakao Address Conversion Failed. Error: {}", e.getMessage(), e);
        }
        return null; // 변환 실패
    }

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