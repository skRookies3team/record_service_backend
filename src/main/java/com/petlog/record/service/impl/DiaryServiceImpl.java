package com.petlog.record.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlog.record.client.ImageClient;
import com.petlog.record.client.PetClient;
import com.petlog.record.client.UserClient;
//import com.petlog.record.dto.DiaryEventMessage;
import com.petlog.record.dto.request.DiaryRequest;
import com.petlog.record.dto.response.AiDiaryResponse;
import com.petlog.record.dto.client.ArchiveResponse;
import com.petlog.record.dto.response.DiaryResponse;
import com.petlog.record.dto.response.DiaryStyleResponse;
import com.petlog.record.entity.*;
import com.petlog.record.entity.mongo.PhotoMetadata;
import com.petlog.record.exception.BusinessException;
import com.petlog.record.exception.EntityNotFoundException;
import com.petlog.record.exception.ErrorCode;
import com.petlog.record.repository.jpa.DiaryArchiveRepository;
import com.petlog.record.repository.jpa.DiaryRepository;
import com.petlog.record.repository.mongo.PhotoMetadataRepository;
import com.petlog.record.service.DiaryService;
import com.petlog.record.service.DiaryStyleService;
import com.petlog.record.service.WeatherService;
import com.petlog.record.util.LatXLngY;

// ✅ Milvus 관련 임포트가 정확히 선언되어야 합니다.
import io.milvus.client.MilvusClient;
import io.milvus.param.R;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.param.collection.FlushParam;

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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import com.petlog.record.infrastructure.kafka.DiaryEventProducer; // ✅ 추가
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryServiceImpl implements DiaryService {

    private final DiaryRepository diaryRepository;
    private final DiaryStyleService diaryStyleService;
    private final DiaryArchiveRepository diaryArchiveRepository;
    private final PhotoMetadataRepository photoMetadataRepository; // ✅ 추가 주입

    private final UserClient userClient;
    private final PetClient petClient;
    private final ImageClient imageClient;

    private final ChatModel chatModel;
    private final WeatherService weatherService;
    private final RestTemplate restTemplate;

    // [Milvus] VectorStore 주입 (Spring AI가 설정파일 기반으로 자동 구성)
    // ✅ application.yml의 설정값을 읽어옵니다. (기본값: vector_store)
    @Value("${spring.ai.vectorstore.milvus.collection-name:vector_store}")
    private String collectionName;

    private final VectorStore vectorStore;

    // ✅ MilvusClient 추가 주입 (Flush 명령용)
    private final MilvusClient milvusClient;

    // ✅ Kafka Producer 주입
    private final DiaryEventProducer diaryEventProducer;

    private final ApplicationEventPublisher eventPublisher; // ✅ 추가

//    // ✅ 1. KafkaTemplate 필드 선언 (이 부분이 없어서 빨간 줄이 뜹니다)
//    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("classpath:prompts/diary-system.st")
    private Resource systemPromptResource;

    @Value("${kakao.rest-api-key}")
    private String kakaoRestApiKey;

    @Override
    @Transactional
    public AiDiaryResponse previewAiDiary(Long userId, Long petId, List<DiaryRequest.Image> images, List<MultipartFile> imageFiles, Double latitude, Double longitude, String date) {
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

        // ✅ [수정] 날짜 정보 보정 및 응답 객체 주입
        LocalDate diaryDate;
        try {
            diaryDate = (date != null && !date.isEmpty()) ? LocalDate.parse(date) : LocalDate.now();
        } catch (DateTimeParseException e) {
            log.warn("Invalid date format: {}, defaulting to today", date);
            diaryDate = LocalDate.now();
        }
        aiResponse.setDate(diaryDate); // ✅ DTO에 날짜 세팅


        // 3. 응답 객체에 이미지 정보 주입 (AiDiaryResponse에 @Setter가 있어야 함)
        aiResponse.setImageUrls(finalImageUrls);
        aiResponse.setArchiveIds(finalArchiveIds);

        // 3. [NEW] 실제 날씨 조회 및 보정
        if (latitude != null && longitude != null) {
            try {
                // 기상청 Grid 변환 (LatXLngY는 기존에 있는 유틸 클래스 사용)
                int[] grid = LatXLngY.convert(latitude, longitude);

                // WeatherService를 통해 실제 날씨 조회
                String realWeather = weatherService.getCurrentWeather(grid[0], grid[1]);

                if (realWeather != null && !realWeather.isEmpty()) {
                    log.info("실제 날씨 조회 성공: {}", realWeather);
                    aiResponse.setWeather(realWeather); // AI가 추측한 날씨를 실제 날씨로 덮어씌움
                }
            } catch (Exception e) {
                log.warn("날씨 조회 실패, AI 추측값 유지: {}", e.getMessage());
            }
        }

        // 4. 위치명(LocationName) 보정 (선택 사항)
        // 만약 프론트에서 보낸 위치 주소를 우선하고 싶다면 파라미터로 locationName도 받아서 여기서 setLocationName() 하면 됩니다.
        // 현재는 AI 추측값 또는 프론트엔드의 로직을 따릅니다.

        return aiResponse;
    }

    @Override
    @Transactional
    public Long saveDiary(DiaryRequest.Create request) {
        log.info("Final Diary Saving. User: {}, Title: {}", request.getUserId(), request.getTitle());
        // 1. 날씨 및 주소 정보 보정
        String weatherInfo = request.getWeather();
        if ((weatherInfo == null || weatherInfo.isEmpty()) && request.getLatitude() != null && request.getLongitude() != null) {
            try {
                int[] grid = LatXLngY.convert(request.getLatitude(), request.getLongitude());
                weatherInfo = weatherService.getCurrentWeather(grid[0], grid[1]);
            } catch (Exception e) {
                weatherInfo = "맑은 날씨";
            }
        }

        String finalLocationName = request.getLocationName();
        if ((finalLocationName == null || finalLocationName.isEmpty()) && request.getLatitude() != null && request.getLongitude() != null) {
            finalLocationName = getAddressFromCoords(request.getLatitude(), request.getLongitude());
        }

        // 2. Diary 엔티티 생성
        Diary diary = Diary.builder()
                .userId(request.getUserId())
                .petId(request.getPetId())
                .title(request.getTitle()) // ✅ 추가
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
        log.info("=== 이미지 처리 시작 ===");
        log.info("request.getImages(): {}", request.getImages());
        log.info("request.getImageUrls(): {}", request.getImageUrls());

        // 3. 이미지 정보 연결
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            // ✅ images 필드 사용 (상세 정보 포함)
            for (DiaryRequest.Image imageDto : request.getImages()) {
                DiaryImage diaryImage = DiaryImage.builder()
                        .imageUrl(imageDto.getImageUrl())
                        .userId(request.getUserId())
                        .imgOrder(imageDto.getImgOrder())
                        .mainImage(imageDto.getMainImage())
                        .source(imageDto.getSource())
                        .build();
                diary.addImage(diaryImage);

                log.info("  ✅ DB에 저장됨: order={}, mainImage={}, source={}",
                        imageDto.getImgOrder(), imageDto.getMainImage(), imageDto.getSource());
            }
        } else if (request.getImageUrls() != null) {
            // 하위 호환성: 기존 방식도 지원
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

        // 4. [추가] MongoDB에 사진별 비정형 메타데이터 저장
        // 4. [수정] MongoDB에 사진별 비정형 메타데이터 저장
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            // 저장된 엔티티들을 URL을 키로 하는 Map으로 변환 (정확한 ID 매핑을 위해)
            Map<String, Long> urlToIdMap = savedDiary.getImages().stream()
                    .collect(Collectors.toMap(DiaryImage::getImageUrl, DiaryImage::getImageId));

            for (DiaryRequest.Image imageDto : request.getImages()) {
                if (imageDto.getMetadata() != null && !imageDto.getMetadata().isEmpty()) {
                    Long savedImageId = urlToIdMap.get(imageDto.getImageUrl());

                    if (savedImageId != null) {
                        PhotoMetadata mongoData = PhotoMetadata.builder()
                                .imageId(savedImageId)
                                .metadata(imageDto.getMetadata())
                                .build();
                        log.info("MongoDB 저장 시도 - imageId: {}", savedImageId);
                        photoMetadataRepository.save(mongoData);
                        log.info("MongoDB 저장 완료");
                    }
                }
            }
        }


        // 4. Diary-Archive 매핑 저장
        if (request.getArchiveIds() != null) {
            for (Long archiveId : request.getArchiveIds()) {
                diaryArchiveRepository.save(DiaryArchive.create(savedDiary, archiveId));
            }
        }

        // 7. [Milvus] 벡터 DB에 일기 내용 저장 (검색/RAG용)
        saveDiaryToVectorDB(savedDiary);

        // ✅ 3. Kafka 전송을 위한 이벤트를 발행 (직접 호출 대신 이벤트만 던짐)
        String firstImageUrl = savedDiary.getImages().isEmpty() ? null : savedDiary.getImages().get(0).getImageUrl();
        eventPublisher.publishEvent(new DiaryCreatedEvent(
                savedDiary.getDiaryId(),
                savedDiary.getUserId(),
                savedDiary.getPetId(),
                savedDiary.getContent(),
                firstImageUrl
        ));

        return savedDiary.getDiaryId();
    }


    /**
     * ✅ 카프카 전송 전용 리스너
     * propagation = Propagation.NOT_SUPPORTED 를 사용하여
     * 클래스 레벨의 @Transactional 설정을 무시하고 트랜잭션 없이 실행되도록 합니다.
     */
    @Async
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDiaryCreatedEvent(DiaryCreatedEvent event) {
        log.info("DB 커밋 완료됨. 카프카 이벤트 발행을 시도합니다. DiaryId: {}", event.diaryId());
        try {
            diaryEventProducer.publishDiaryCreatedEvent(
                    event.diaryId(), event.userId(), event.petId(), event.content(), event.imageUrl()
            );
        } catch (Exception e) {
            log.error("Kafka 전송 실패 (하지만 DB는 이미 저장됨): {}", e.getMessage());
        }
    }

    // 이벤트 객체 정의 (간단하게 레코드로 작성)
    public record DiaryCreatedEvent(Long diaryId, Long userId, Long petId, String content, String imageUrl) {}


    /**
     * [Milvus] 생성된 일기를 벡터 DB에 저장하는 메서드
     * Spring AI의 Document 객체로 변환하여 VectorStore에 저장합니다.
     */
    private void saveDiaryToVectorDB(Diary diary) {
        try {
            log.info("Milvus Vector DB 저장 시작 (컬렉션: {}) - DiaryId: {}", collectionName, diary.getDiaryId());


            // 1. 메타데이터 생성 (검색 시 필터링에 사용할 데이터)
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("userId", diary.getUserId());
            metadata.put("petId", diary.getPetId());
            metadata.put("diaryId", diary.getDiaryId());
            metadata.put("title", diary.getTitle()); // ✅ 검색 필터링/결과용 추가
            metadata.put("date", diary.getDate().toString());
            metadata.put("mood", diary.getMood());
            if (diary.getWeather() != null) metadata.put("weather", diary.getWeather());
            if (diary.getLocationName() != null) metadata.put("location", diary.getLocationName());

            // 2. Document 생성 (내용 + 메타데이터)
            // Embedding은 VectorStore가 설정된 모델(OpenAI 등)을 사용해 자동으로 수행함
            Document document = new Document(diary.getContent(), metadata);

            // 3. 저장
            vectorStore.add(List.of(document));

            // ✅ 2. 강제로 디스크에 쓰기 (Flush) - MinIO 용량 확인용
            // 'spring_ai_vectors'는 Spring AI Milvus VectorStore의 기본 컬렉션명입니다.
            // ✅ 2. 안전한 Flush 호출 (hasCollection 성공 시에만)
            if (hasCollection(collectionName)) {
                milvusClient.flush(FlushParam.newBuilder()
                        .addCollectionName(collectionName)
                        .build());
                log.info("Milvus 저장 및 Flush 완료: {}", collectionName);
            }

            log.info("Milvus 저장 완료");

        } catch (Exception e) {
            // 벡터 DB 저장이 실패해도 메인 트랜잭션(RDB 저장)은 롤백되지 않도록 로그만 남김 (선택 사항)
            log.error("Milvus Vector DB 저장 실패: {}", e.getMessage(), e);
            // 필요 시 throw new BusinessException(ErrorCode.VECTOR_STORE_ERROR);
        }
    }

    /**
     * ✅ Milvus 컬렉션 존재 여부 확인 (빨간 줄 해결 버전)
     */
    private boolean hasCollection(String name) {
        try {
            // Milvus SDK의 응답은 R<Boolean> 타입입니다.
            R<Boolean> response = milvusClient.hasCollection(HasCollectionParam.newBuilder()
                    .withCollectionName(name)
                    .build());

            // 응답이 성공(Success)이고 데이터가 true일 때만 true 반환
            return response.getStatus() == R.Status.Success.getCode() && Boolean.TRUE.equals(response.getData());
        } catch (Exception e) {
            log.error("Milvus 컬렉션 확인 중 오류 발생: {}", e.getMessage());
            return false;
        }
    }


    private void autoArchiveDiaryImages(Long userId, List<MultipartFile> imageFiles) {
        try {
            imageClient.createArchive(userId, imageFiles);
            log.info("사용자 {}의 보관함에 이미지 자동 저장 요청 완료", userId);
        } catch (Exception e) {
            log.warn("보관함 자동 저장 실패: {}", e.getMessage());
        }
    }


    // AI 분석 헬퍼 메서드 (URL 기반)
    private AiDiaryResponse generateContentWithAiFromUrls(List<String> imageUrls) {
        BeanOutputConverter<AiDiaryResponse> converter = new BeanOutputConverter<>(AiDiaryResponse.class);

        // 1. 기본 시스템 프롬프트 렌더링
        String baseSystemPrompt = new PromptTemplate(systemPromptResource).render();
        //SystemMessage systemMessage = new SystemMessage(baseSystemPrompt + "\n\n보관함의 사진들을 분석하여 일기를 작성하세요.");
        String customInstruction = "\n\n" +
                "1. 사진의 상황을 파악하여 감성적이고 잘 어울리는 일기 제목(title)을 생성하세요.\n" +
                "2. 보관함의 사진들을 분석하여 일기 내용(content)을 작성하세요.";

        SystemMessage systemMessage = new SystemMessage(baseSystemPrompt + customInstruction);

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
        // 1. PostgreSQL에서 일기 조회
        Diary diary = diaryRepository.findById(diaryId).orElseThrow(() -> new EntityNotFoundException(ErrorCode.DIARY_NOT_FOUND));

        // 2. 이미지 ID 목록 추출
        List<Long> imageIds = diary.getImages().stream()
                .map(DiaryImage::getImageId)
                .toList();

        // 3. MongoDB에서 해당 이미지들의 메타데이터 일괄 조회
        // 3. MongoDB 메타데이터 조회
        Map<Long, Map<String, Object>> metadataMap = photoMetadataRepository.findAllByImageIdIn(imageIds)
                .stream()
                .collect(Collectors.toMap(
                        PhotoMetadata::getImageId,
                        PhotoMetadata::getMetadata,
                        (existing, replacement) -> existing // 중복 키 방지
                ));

        // 4. Entity -> DTO 변환 시 메타데이터 주입
        List<DiaryResponse.Image> imageDtos = diary.getImages().stream()
                .map(img -> DiaryResponse.Image.fromEntity(img, metadataMap.get(img.getImageId())))
                .toList();

        // 2. Entity -> DTO 변환
        DiaryResponse response = DiaryResponse.fromEntity(diary);

        response.setImages(imageDtos); // 메타데이터가 포함된 이미지 리스트로 교체

        // 3. ✅ 스타일 조회 및 설정
        try {
            DiaryStyleResponse styleResponse = diaryStyleService.getDiaryStyle(diaryId);
            response.setStyle(styleResponse);
        } catch (Exception e) {
            // 스타일이 없어도 다이어리 조회는 성공
            log.warn("No style found for diary {}", diaryId);
            response.setStyle(null);
        }
        return response;
    }


    @Override
    @Transactional
    public void updateDiary(Long diaryId, DiaryRequest.Update request) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.DIARY_NOT_FOUND));

        // 1. 엔티티 수정 (제목 필드 반영)
        // Diary 엔티티의 update 메서드가 (title, content, visibility, weather, mood) 순서라고 가정합니다.
        diary.update(
                request.getTitle(),
                request.getContent(),
                request.getDate(),
                request.getVisibility(),
                request.getWeather(),
                request.getMood()
        );

        // 2. [Milvus] 벡터 DB 정보 갱신
        // 제목이나 내용이 바뀌었으므로 임베딩을 다시 생성하여 저장합니다.
        // Spring AI의 VectorStore.add는 보통 같은 ID(metadata의 diaryId)를 기반으로 덮어쓰기하거나 새로 생성합니다.
        saveDiaryToVectorDB(diary);

        // 3. [Kafka] 수정 이벤트 발행
        try {
            diaryEventProducer.publishDiaryUpdatedEvent(
                    diary.getDiaryId(),
                    diary.getUserId(),
                    diary.getPetId(),
                    diary.getContent()
            );
        } catch (Exception e) {
            log.error("Kafka 수정 이벤트 발행 실패: {}", e.getMessage());
        }
    }

//    @Override
//    @Transactional
//    public void deleteDiary(Long diaryId) {
//        Diary diary = diaryRepository.findById(diaryId).orElseThrow(() -> new EntityNotFoundException(ErrorCode.DIARY_NOT_FOUND));
//        diaryRepository.delete(diary);
//
//        // ✅ Kafka 삭제 이벤트 발행
//        diaryEventProducer.publishDiaryDeletedEvent(diaryId, userId, petId);
//    }

    @Override
    @Transactional
    public void deleteDiary(Long diaryId) {
        // 1. 삭제 전 엔티티 조회 (데이터가 있어야 정보를 추출할 수 있음)
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.DIARY_NOT_FOUND));

        // 2. Kafka 전송에 필요한 정보 미리 추출
        Long userId = diary.getUserId();
        Long petId = diary.getPetId();

        // MongoDB 데이터 삭제를 위해 이미지 ID 추출
        List<Long> imageIds = diary.getImages().stream()
                .map(DiaryImage::getImageId)
                .toList();

        // 3. MongoDB 메타데이터 삭제
        photoMetadataRepository.deleteAllByImageIdIn(imageIds);

        // 4. DB에서 삭제
        // 4. PostgreSQL 데이터 삭제
        diaryRepository.delete(diary);

        // 5. ✅ Kafka 삭제 이벤트 발행 (추출한 변수 사용)
        try {
            diaryEventProducer.publishDiaryDeletedEvent(diaryId, userId, petId);
            log.info("Diary 삭제 이벤트 발행 성공: diaryId {}", diaryId);
        } catch (Exception e) {
            log.error("Diary 삭제 이벤트 발행 실패: {}", e.getMessage());
        }
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
        } catch (Exception e) { log.error("주소 변환 실패"); }
        return null;
    }
}