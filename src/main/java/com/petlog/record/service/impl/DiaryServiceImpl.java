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
import org.springframework.ai.chat.messages.SystemMessage;
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
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
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
    public Long createAiDiary(Long userId, Long petId, Long photoArchiveId, List<DiaryRequest.Image> images, List<MultipartFile> imageFiles,
                              Visibility visibility, String locationName,
                              Double latitude, Double longitude, LocalDate date) {

        log.info("AI Diary creation started. User: {}, Pet: {}", userId, petId);

        validateUserAndPet(userId, petId);

        List<String> imageUrls = new ArrayList<>();
        ImageSource source;

        // 1. 이미지 소스 판별
        // photoArchiveId가 있고, images 리스트에 URL이 들어있다면 보관함 모드!
        if (photoArchiveId != null && images != null && !images.isEmpty()) {
            source = ImageSource.ARCHIVE;
            // DTO 내부의 Image 객체들에서 URL만 추출
            imageUrls = images.stream()
                    .map(DiaryRequest.Image::getImageUrl)
                    .collect(Collectors.toList());
            log.info("Source: ARCHIVE - 프론트엔드 전달 URL 사용: {}", imageUrls);
        }
        // 보관함 ID가 없고 파일이 직접 들어왔다면 갤러리 모드!
        else if (isActualFilePresent(imageFiles)) {
            source = ImageSource.GALLERY;
            try {
                ArchiveResponse.CreateArchiveDtoList archiveResponse = imageClient.createArchive(userId, imageFiles);
                imageUrls = archiveResponse.getArchives().stream()
                        .map(ArchiveResponse.CreateArchiveDto::getUrl)
                        .collect(Collectors.toList());
            } catch (Exception e) {
                throw new RuntimeException("이미지 서버 연동 실패");
            }
        } else {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        }

        // 2. AI 일기 내용 생성 (기존 로직 유지 - 소스 구별 없이 분석 수행)
        // 만약 ARCHIVE 소스라서 imageFiles가 비어있을 경우를 대비해 imageUrls 기반 분석 로직을 활용하거나
        // 클라이언트에서 분석을 위해 파일을 항상 보내준다면 기존 generateContentWithAi(imageFiles)를 그대로 사용합니다.
        AiDiaryResponse aiResponse;
//        if (imageFiles != null && !imageFiles.isEmpty()) {
//            aiResponse = generateContentWithAi(imageFiles);
//        } else {
//            // 보관함 선택 시 파일 데이터가 없다면 URL을 통해 분석하는 별도 로직이 필요할 수 있으나,
//            // 요청에 따라 기존 분석 코드의 흐름을 최대한 유지합니다.
//            aiResponse = generateContentWithAiFromUrls(imageUrls);
//        }
        if (source == ImageSource.GALLERY) {
            aiResponse = generateContentWithAi(imageFiles);
        } else {
            // 보관함 사진일 경우 URL로 분석
            aiResponse = generateContentWithAiFromUrls(imageUrls);
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

        // 5. Diary 엔티티 생성
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

        // 6. 이미지 목록을 순회하며 DiaryImage 생성 및 Diary에 추가 (다이어리 서비스 DB에만 저장)
        for (int i = 0; i < imageUrls.size(); i++) {
            DiaryImage diaryImage = DiaryImage.builder()
                    .imageUrl(imageUrls.get(i))
                    .userId(userId)
                    .imgOrder(i + 1)
                    .mainImage(i == 0)
                    .source(source)
                    .build();

            diary.addImage(diaryImage);
        }

        Diary savedDiary = diaryRepository.save(diary);

        return savedDiary.getDiaryId();
    }

    /**
     * 여러 이미지를 분석하여 AI 일기 생성 (MultipartFile 기반)
     */
    private AiDiaryResponse generateContentWithAi(List<MultipartFile> imageFiles) {
        BeanOutputConverter<AiDiaryResponse> converter = new BeanOutputConverter<>(AiDiaryResponse.class);
        String baseSystemPrompt = new PromptTemplate(systemPromptResource).render();

        String multiImageInstruction = String.format(
                "\n\n[중요 지시사항]\n" +
                        "현재 사용자가 총 %d장의 사진을 업로드했습니다.\n" +
                        "1. 모든 사진을 순서대로 분석하여 하나의 연결된 스토리를 만드세요.\n" +
                        "2. 특정 사진 한 장에만 집중하지 말고, 반려동물의 다양한 행동을 일기에 포함하세요.",
                imageFiles.size()
        );

        SystemMessage systemMessage = new SystemMessage(baseSystemPrompt + multiImageInstruction);

        try {
            List<Media> mediaList = new ArrayList<>();
            for (MultipartFile file : imageFiles) {
                mediaList.add(new Media(MimeTypeUtils.IMAGE_JPEG, new ByteArrayResource(file.getBytes())));
            }

            String userInstruction = "제공된 이미지들을 분석하여 정해진 JSON 형식으로 응답하세요.\n" + converter.getFormat();
            UserMessage userMessage = new UserMessage(userInstruction, mediaList);

            Prompt prompt = new Prompt(List.of(systemMessage, userMessage), OpenAiChatOptions.builder().withModel("gpt-4o").build());
            String responseContent = chatModel.call(prompt).getResult().getOutput().getContent();

            return converter.convert(responseContent);
        } catch (Exception e) {
            log.error("AI 멀티 이미지 분석 중 오류 발생", e);
            throw new RuntimeException("AI 일기 생성 실패");
        }
    }

    // 헬퍼 메서드: 실제 유효한 파일이 있는지 체크
    private boolean isActualFilePresent(List<MultipartFile> files) {
        return files != null && !files.isEmpty() && !files.get(0).isEmpty();
    }

    /**
     * 이미지 URL을 분석하여 AI 일기 생성 (URL 기반 - ARCHIVE 소스용)
     */
    private AiDiaryResponse generateContentWithAiFromUrls(List<String> imageUrls) {
        BeanOutputConverter<AiDiaryResponse> converter = new BeanOutputConverter<>(AiDiaryResponse.class);
        String baseSystemPrompt = new PromptTemplate(systemPromptResource).render();
        SystemMessage systemMessage = new SystemMessage(baseSystemPrompt + "\n\n보관함의 사진들을 분석하여 일기를 작성하세요.");

        try {
            List<Media> mediaList = new ArrayList<>();
            for (String url : imageUrls) {
                mediaList.add(new Media(MimeTypeUtils.IMAGE_JPEG, new URL(url)));
            }

            String userInstruction = "제공된 이미지 URL들을 분석하여 JSON 형식으로 응답하세요.\n" + converter.getFormat();
            UserMessage userMessage = new UserMessage(userInstruction, mediaList);

            Prompt prompt = new Prompt(List.of(systemMessage, userMessage), OpenAiChatOptions.builder().withModel("gpt-4o").build());
            return converter.convert(chatModel.call(prompt).getResult().getOutput().getContent());
        } catch (Exception e) {
            log.error("AI URL 분석 중 오류 발생", e);
            throw new RuntimeException("AI 일기 생성 실패");
        }
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
}