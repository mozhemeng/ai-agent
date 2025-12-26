package org.example.aiagent.service.impl;

import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.example.aiagent.dto.ChatResponseDTO;
import org.example.aiagent.service.ChatService;
import org.example.aiagent.service.LlmService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    @Value("${llm.model-name}")
    private String modelName;

    private final LlmService llmService;

    @Override
    public ChatResponseDTO chat(String prompt) {
        ChatCompletionCreateParams params = buildChatParams(prompt);
        List<ChatCompletionChunk> chunks = llmService.execute(params);

        return chunkListToChatResponse(chunks);
    }

    @Override
    public Flux<ChatResponseDTO> streamChat(String prompt) {
        ChatCompletionCreateParams params = buildChatParams(prompt);

        return llmService.executeStreaming(params)
                .map(this::chunkToChatResponse)
                // 过滤掉没有内容的消息
                .filter(dto -> dto.getContent() != null || dto.getReasoning() != null);
    }

    private ChatCompletionCreateParams buildChatParams(String prompt) {

        return ChatCompletionCreateParams.builder()
                .addUserMessage(prompt)
                .model(modelName)
                .build();
    }

    private ChatResponseDTO chunkListToChatResponse(List<ChatCompletionChunk> chunks) {
        StringBuilder contentSb = new StringBuilder();
        StringBuilder reasonSb = new StringBuilder();
        for (ChatCompletionChunk chunk : chunks) {
            chunk.choices().stream().findFirst().ifPresent(choice -> {
                choice.delta().content().ifPresent(content -> {
                    if (StringUtils.isNotBlank(content)) {
                        contentSb.append(content);
                    }
                });
                Optional.of(choice.delta()._additionalProperties()).ifPresent(additionalProperties -> {
                    if (additionalProperties.containsKey("reasoning_content") && !additionalProperties.get("reasoning_content").isNull()) {
                        reasonSb.append(additionalProperties.get("reasoning_content").toString());
                    }
                });
            });
        }
        ChatResponseDTO chatResponseDTO = new ChatResponseDTO();
        chatResponseDTO.setContent(contentSb.toString());
        chatResponseDTO.setReasoning(reasonSb.toString());

        return chatResponseDTO;
    }

    private ChatResponseDTO chunkToChatResponse(ChatCompletionChunk chunk) {
        ChatResponseDTO llmResponse = new ChatResponseDTO();
        chunk.choices().stream().findFirst().ifPresent(choice -> {
            choice.delta().content().ifPresent(content -> {
                if (StringUtils.isNotBlank(content)) {
                    llmResponse.setContent(content);
                }
            });
            Optional.of(choice.delta()._additionalProperties()).ifPresent(additionalProperties -> {
                if (additionalProperties.containsKey("reasoning_content") && !additionalProperties.get("reasoning_content").isNull()) {
                    llmResponse.setReasoning(additionalProperties.get("reasoning_content").toString());
                }
            });
        });
        return llmResponse;
    }
}
