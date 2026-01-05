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
 * AI 분석 전담 서비스
 * Spring AI를 사용하여 이미지 분석 및 일기 초안 생성 담당
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiaryAiService {

    private final ChatModel chatModel;

    @Value("classpath:prompts/diary-system.st")
    private Resource systemPromptResource;

    public AiDiaryResponse generateContentWithAiFromUrls(List<String> imageUrls) {
        BeanOutputConverter<AiDiaryResponse> converter = new BeanOutputConverter<>(AiDiaryResponse.class);

        String baseSystemPrompt = new PromptTemplate(systemPromptResource).render();
        String customInstruction = "\n\n1. 사진의 상황을 파악하여 감성적이고 잘 어울리는 일기 제목(title)을 생성하세요.\n" +
                "2. 보관함의 사진들을 분석하여 일기 내용(content)을 작성하세요.";

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
                    OpenAiChatOptions.builder().withModel("gpt-4o").build()
            );

            return converter.convert(chatModel.call(prompt).getResult().getOutput().getContent());
        } catch (Exception e) {
            log.error("AI Analysis Failed", e);
            throw new RuntimeException("AI 일기 생성 실패");
        }
    }
}