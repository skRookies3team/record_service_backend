package com.petlog.record.service.impl;

import com.petlog.record.dto.response.RecapAiResponse;
import com.petlog.record.service.RecapAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RecapAiServiceImpl implements RecapAiService {

    private final ChatModel chatModel;

    @Override
    public RecapAiResponse analyzeMonth(String petName, int year, int month, List<String> diaryEntries) {
        BeanOutputConverter<RecapAiResponse> converter = new BeanOutputConverter<>(RecapAiResponse.class);

        String userPromptText = """
                반려동물 {petName}의 {year}년 {month}월 일기 기록들입니다:
                {diaries}
                
                위 기록들을 바탕으로 이번 달의 '월간 리캡'을 작성해주세요.
                
                [작성 지침]
                1. 제목(title): 반드시 {year}년 {month}월이라는 구체적인 숫자를 포함하여 '2024년 3월의 소중한 기록'과 같은 스타일로 지어주세요. (202X년 X월과 같은 플레이스홀더를 사용하지 마세요.)
                2. 요약(summary): 전체적인 일기 내용을 분석하여 보호자에게 보내는 편지 형식으로 따뜻하게 작성해주세요.
                3. 하이라이트(highlights): 가장 인상 깊은 사건 3가지를 선정해 제목과 요약을 작성해주세요.
                
                {format}
                """;

        PromptTemplate promptTemplate = new PromptTemplate(userPromptText);
        Prompt prompt = promptTemplate.create(Map.of(
                "petName", petName,
                "year", year,
                "month", month,
                "diaries", String.join("\n---\n", diaryEntries),
                "format", converter.getFormat()
        ));

        var response = chatModel.call(prompt);
        return converter.convert(response.getResult().getOutput().getContent());
    }
}