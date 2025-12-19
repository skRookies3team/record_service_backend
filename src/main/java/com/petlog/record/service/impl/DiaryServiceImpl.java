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
    public Long createAiDiary(Long userId, Long petId, Long photoArchiveId, List<MultipartFile> imageFiles,
                              Visibility visibility, String locationName,
                              Double latitude, Double longitude, LocalDate date) {

        log.info("AI Diary creation started for user: {}, pet: {}, images: {}장", userId, petId, imageFiles.size());

        validateUserAndPet(userId, petId);

        // 1. 유저 서비스(8080) 호출하여 S3에 여러 장 업로드
        List<String> uploadedImageUrls;
        try {
            // imageClient를 통해 파일 리스트 전체를 전달
            uploadedImageUrls = imageClient.uploadImageToS3(imageFiles);
            if (uploadedImageUrls == null || uploadedImageUrls.isEmpty()) {
                throw new BusinessException(ErrorCode.UPLOAD_FILE_IO_EXCEPTION);
            }
            log.info("S3 업로드 완료: {}개의 URL 획득", uploadedImageUrls.size());
        } catch (Exception e) {
            log.error("S3 업로드 호출 실패: {}", e.getMessage());
            throw new RuntimeException("이미지 서버 연동 실패");
        }

        // 2. AI 일기 내용 생성 (분석은 첫 번째 이미지를 기준으로 수행)
        AiDiaryResponse aiResponse;
        try {
            byte[] firstImageBytes = imageFiles.get(0).getBytes();
            aiResponse = generateContentWithAi(firstImageBytes);
        } catch (IOException e) {
            log.error("파일 데이터를 읽는 중 오류 발생", e);
            throw new BusinessException(ErrorCode.UPLOAD_FILE_IO_EXCEPTION);
        }

        // 3. 날씨 정보 처리
        String weatherInfo = "맑음";
        LocalDate targetDate = date != null ? date : LocalDate.now();
        try {
            if (latitude != null && longitude != null && latitude != 0 && longitude != 0) {
                int[] grid = LatXLngY.convert(latitude, longitude);
                weatherInfo = weatherService.getCurrentWeather(grid[0], grid[1]);
            }
        } catch (Exception e) {
            log.warn("Weather API call failed: {}", e.getMessage());
        }

        // 4. 주소 변환
        String finalLocationName = locationName;
        if ((finalLocationName == null || finalLocationName.isEmpty()) && latitude != null && longitude != null) {
            finalLocationName = getAddressFromCoords(latitude, longitude);
        }

        // 5. 일기(Diary) 엔티티 생성 및 이미지(DiaryImage) 목록 추가
        Diary diary = Diary.builder()
                .userId(userId)
                .petId(petId)
                .photoArchiveId(photoArchiveId)
                .content(aiResponse.getContent())
                .mood(aiResponse.getMood())
                .weather(weatherInfo)
                .isAiGen(true)
                .visibility(visibility != null ? visibility : Visibility.PRIVATE)
                .latitude(latitude)
                .longitude(longitude)
                .locationName(finalLocationName != null ? finalLocationName : "위치 정보 없음")
                .date(targetDate)
                .build();

        // [핵심] 업로드된 모든 URL을 순회하며 DiaryImage 엔티티를 생성하고 Diary에 추가
        for (int i = 0; i < uploadedImageUrls.size(); i++) {
            DiaryImage diaryImage = DiaryImage.builder()
                    .imageUrl(uploadedImageUrls.get(i))
                    .userId(userId)
                    .imgOrder(i + 1)
                    .mainImage(i == 0) // 첫 번째 이미지를 대표(메인) 이미지로 설정
                    .source(ImageSource.GALLERY)
                    .build();

            // Diary 엔티티 내부의 List<DiaryImage>에 추가 (CascadeType.ALL에 의해 함께 저장됨)
            diary.addImage(diaryImage);
        }

        Diary savedDiary = diaryRepository.save(diary);

        // 6. 보관함(Archive) 서비스로 자동 저장 연동
        autoArchiveDiaryImages(savedDiary.getUserId(), imageFiles);

        return savedDiary.getDiaryId();
    }

    private void autoArchiveDiaryImages(Long userId, List<MultipartFile> imageFiles) {
        try {
            imageClient.createArchive(userId, imageFiles);
            log.info("사용자 {}의 보관함에 이미지 자동 저장 요청 완료", userId);
        } catch (Exception e) {
            log.warn("보관함 자동 저장 실패: {}", e.getMessage());
        }
    }

    private AiDiaryResponse generateContentWithAi(byte[] imageBytes) {
        BeanOutputConverter<AiDiaryResponse> converter = new BeanOutputConverter<>(AiDiaryResponse.class);
        String systemPromptText = new PromptTemplate(systemPromptResource).render();
        String promptText = systemPromptText + "\n\n" + converter.getFormat();

        try {
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
        try { userClient.getUserInfo(userId); } catch (Exception e) { throw new EntityNotFoundException(ErrorCode.USER_NOT_FOUND); }
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
                    if ("H".equals(doc.path("region_type").asText())) return doc.path("address_name").asText();
                }
                return documents.get(0).path("address_name").asText();
            }
        } catch (Exception e) {
            log.error("Kakao Address Conversion Failed");
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
        diary.update(request.getContent(), request.getVisibility(), request.getWeather(), request.getMood());
    }

    @Override
    @Transactional
    public void deleteDiary(Long diaryId) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.DIARY_NOT_FOUND));
        diaryRepository.delete(diary);
    }
}