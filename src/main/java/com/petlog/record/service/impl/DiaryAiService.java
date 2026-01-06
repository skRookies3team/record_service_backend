package com.petlog.record.service.impl;

import com.petlog.record.dto.response.AiDiaryResponse;
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
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * [AI 분석 및 일기 생성 서비스]
 * Spring AI 프레임워크를 활용하여 멀티모달(이미지+텍스트) 분석을 수행
 * 사진의 맥락을 읽어 반려동물의 시점에서 개성 있는 일기 초안을 생성하는 핵심 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiaryAiService {

    private final ChatModel chatModel;

    @Value("classpath:prompts/diary-system.st")
    private Resource systemPromptResource;

    /**
     * [이미지 기반 AI 일기 초안 생성]
     * 전달받은 이미지 URL 목록을 분석하여 감성적인 제목, 본문, 날씨, 기분 등을 포함한 JSON 데이터 생성
     * * [주요 프롬프트 전략]
     * 1. 스토리 다변화: 뻔한 전개(준비-활동-복귀)를 지양하고 상황에 맞는 다양한 서사 구조 채택
     * 2. 감각 묘사: 오감(냄새, 소리 등) 중심의 생생한 서술 및 반려동물 특유의 시각 반영
     * 3. 출력 규격: BeanOutputConverter를 통해 LLM 응답을 AiDiaryResponse 객체로 즉시 구조화
     * * @param imageUrls 분석할 이미지 S3 URL 리스트
     * @return AI 분석 결과 객체 (AiDiaryResponse)
     */
    public AiDiaryResponse generateContentWithAiFromUrls(List<String> imageUrls) {
        BeanOutputConverter<AiDiaryResponse> converter = new BeanOutputConverter<>(AiDiaryResponse.class);

        String baseSystemPrompt = new PromptTemplate(systemPromptResource).render();

        // ✅ 스토리의 다양성을 확보하기 위해 지시 사항을 고도화
        String customInstruction = "\n\n" +
                "[DETAILED INSTRUCTIONS]\n" +
                "1. 제목(title): 사진의 상황을 파악하여 감성적이고 위트 있는 제목을 생성하세요.\n" +
                "2. 내용(content): 약 20문장 내외의 풍성한 장문으로 작성하되, **매번 똑같은 전개 방식(준비-이동-활동-복귀)을 절대 반복하지 마세요.**\n" +
                "3. 구성의 다변화: 사진 속 상황에 따라 어떤 날은 강렬한 사건 중심으로, 어떤 날은 깊은 감성 독백으로, 어떤 날은 주인과의 대화 중심으로 구성을 매번 다르게 가져가세요. 시작점 역시 사진에서 가장 인상 깊은 지점부터 자유롭게 시작하세요.\n" +
                "4. 스토리텔링: 사진들 사이의 연관성을 찾되, 뻔한 흐름이 아닌 반려동물 특유의 엉뚱하거나 순수한 시각이 돋보이는 개성 있는 이야기를 만드세요.\n" +
                "5. 묘사: 오감(냄새, 소리, 촉감) 중 그날 가장 도드라지는 감각에 집중하여 서술하세요.\n" +
                "6. 말투: '~했다', '~했어' 등 친근한 구어체를 사용하고, 개성 있는 의성어/의태어와 이모지를 활용하세요.\n" +
                "7. 금기사항: '사진에는...', '첫 번째는...' 과 같은 설명조를 피하고, 독자가 현장에 있는 것처럼 느끼게 하세요.\n" +
                "8. 기분(mood): 그날 하루를 관통하는 독특한 감정 단어를 추출하세요.";;

        SystemMessage systemMessage = new SystemMessage(baseSystemPrompt + customInstruction);

        try {
            List<Media> mediaList = new ArrayList<>();
            for (String url : imageUrls) {
                mediaList.add(new Media(MimeTypeUtils.IMAGE_JPEG, new URL(url)));
            }

            UserMessage userMessage = new UserMessage(
                    "분석하여 JSON 형식으로 응답하세요.\n" + converter.getFormat(),
                    mediaList
            );

            Prompt prompt = new Prompt(
                    List.of(systemMessage, userMessage),
                    OpenAiChatOptions.builder().withModel("gpt-4o").withTemperature(0.5).build()
            );

            return converter.convert(chatModel.call(prompt).getResult().getOutput().getContent());
        } catch (Exception e) {
            log.error("AI Analysis Failed", e);
            throw new RuntimeException("AI 일기 생성 실패");
        }
    }
}