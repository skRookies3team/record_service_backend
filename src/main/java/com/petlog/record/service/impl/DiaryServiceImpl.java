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
    public Long createAiDiary(Long userId, Long petId, Long photoArchiveId, MultipartFile imageFile,
                              Visibility visibility, String locationName,
                              Double latitude, Double longitude, LocalDate date) {
        log.info("AI Diary creation started for user: {}, pet: {}", userId, petId);

        validateUserAndPet(userId, petId);

        // [중요] MultipartFile의 InputStream은 한 번만 읽을 수 있으므로 byte[]로 복사하여 재사용합니다.
        byte[] imageBytes;
        try {
            imageBytes = imageFile.getBytes();
        } catch (IOException e) {
            log.error("파일 데이터를 읽는 중 오류 발생", e);
            throw new BusinessException(ErrorCode.UPLOAD_FILE_IO_EXCEPTION);
        }

        // 1. 유저 서비스(8080)의 S3 업로드 API 호출
        String uploadedImageUrl;
        try {
            // S3 업로드 (유저 서비스 호출)
            List<String> imageUrls = imageClient.uploadImageToS3(List.of(imageFile));
            if (imageUrls == null || imageUrls.isEmpty()) {
                throw new BusinessException(ErrorCode.UPLOAD_FILE_IO_EXCEPTION);
            }
            uploadedImageUrl = imageUrls.get(0);
            log.info("S3 업로드 성공: {}", uploadedImageUrl);
        } catch (Exception e) {
            log.error("유저 서비스 S3 업로드 호출 실패: {}", e.getMessage());
            throw new RuntimeException("이미지 서버 연동 실패");
        }

        // 2. AI 일기 내용 생성 (복사해둔 byte[] 전달)
        AiDiaryResponse aiResponse = generateContentWithAi(imageBytes);

        // 3. 날씨 정보 처리
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

        // 4. 주소 변환
        String finalLocationName = locationName;
        if ((finalLocationName == null || finalLocationName.isEmpty()) && latitude != null && longitude != null && latitude != 0 && longitude != 0) {
            log.info("좌표 기반 주소 변환 시도: lat={}, lng={}", latitude, longitude);
            finalLocationName = getAddressFromCoords(latitude, longitude);
            log.info("주소 변환 결과: {}", finalLocationName);
        } else if (finalLocationName == null) {
            finalLocationName = "위치 정보 없음";
        }

        // 5. 일기 저장
        Diary diary = Diary.builder()
                .userId(userId)
                .petId(petId)
                .photoArchiveId(photoArchiveId) // [추가] DTO에서 받은 보관함 ID 설정
                .content(aiResponse.getContent())
                .mood(aiResponse.getMood())
                .weather(weatherInfo)
                .isAiGen(true)
                .visibility(visibility)
                .latitude(latitude)
                .longitude(longitude)
                .locationName(finalLocationName)
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

        // 6. 보관함(Archive) 서비스로 MultipartFile 직접 전송 자동화
        autoArchiveDiaryImage(savedDiary.getUserId(), imageFile);

        return savedDiary.getDiaryId();
    }

    /**
     * 일기 이미지를 보관함에 자동으로 추가하는 메서드
     */
    private void autoArchiveDiaryImage(Long userId, MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) return;

        try {
            imageClient.createArchive(userId, List.of(imageFile));
            log.info("사용자 {}의 보관함에 일기 이미지가 자동 저장되었습니다.", userId);
        } catch (Exception e) {
            log.warn("보관함 자동 저장 실패 (사용자: {}): {}", userId, e.getMessage());
        }
    }

    // [수정된 부분] 파라미터를 byte[]로 변경하여 스코프 및 타입 이슈 해결
    private AiDiaryResponse generateContentWithAi(byte[] imageBytes) {
        BeanOutputConverter<AiDiaryResponse> converter = new BeanOutputConverter<>(AiDiaryResponse.class);
        String systemPromptText = new PromptTemplate(systemPromptResource).render();
        String promptText = systemPromptText + "\n\n" + converter.getFormat();

        try {
            // ByteArrayResource를 사용하여 복사된 이미지 데이터 활용
            Media imageMedia = new Media(MimeTypeUtils.IMAGE_JPEG, new ByteArrayResource(imageBytes));
            UserMessage userMessage = new UserMessage(promptText, List.of(imageMedia));
            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .withTemperature(0.7)
                    .withModel("gpt-4o")
                    .build();

            Prompt prompt = new Prompt(userMessage, options);
            String responseContent = chatModel.call(prompt).getResult().getOutput().getContent();
            return converter.convert(responseContent);
        } catch (Exception e) {
            log.error("AI 생성 중 오류 발생", e);
            throw new RuntimeException("AI 생성 실패", e);
        }
    }

    private void validateUserAndPet(Long userId, Long petId) {
        try { userClient.getUserInfo(userId); } catch (FeignException e) { throw new EntityNotFoundException(ErrorCode.USER_NOT_FOUND); }
        try { petClient.getPetInfo(petId); } catch (FeignException e) { throw new EntityNotFoundException(ErrorCode.PET_NOT_FOUND); }
    }

    private String getAddressFromCoords(Double lat, Double lng) {
        try {
            if (kakaoRestApiKey == null || kakaoRestApiKey.isEmpty()) return null;

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
                    if ("H".equals(doc.path("region_type").asText())) {
                        return doc.path("address_name").asText();
                    }
                }
                return documents.get(0).path("address_name").asText();
            }
        } catch (Exception e) {
            log.error("Kakao Address Conversion Failed: {}", e.getMessage());
        }
        return null;
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