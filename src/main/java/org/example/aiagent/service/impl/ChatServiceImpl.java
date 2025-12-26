package org.example.aiagent.service.impl;

import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionStreamOptions;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.example.aiagent.dto.ChatRequestDTO;
import org.example.aiagent.dto.ChatResponseDTO;
import org.example.aiagent.entity.ChatMessage;
import org.example.aiagent.entity.ChatSession;
import org.example.aiagent.enums.MessageRoleEnum;
import org.example.aiagent.service.ChatMessageService;
import org.example.aiagent.service.ChatService;
import org.example.aiagent.service.ChatSessionService;
import org.example.aiagent.service.LlmService;
import org.example.aiagent.util.UuidUtil;
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

    private final ChatSessionService chatSessionService;

    private final ChatMessageService chatMessageService;

    @Override
    public ChatResponseDTO chat(ChatRequestDTO chatRequest) {
        boolean exists = chatSessionService.lambdaQuery()
                .eq(ChatSession::getId, chatRequest.getSessionId())
                .exists();
        if (!exists) {
            // 是会话的第一条消息，创建新会话
            ChatSession chatSession = new ChatSession();
            chatSession.setId(chatRequest.getSessionId());
            chatSession.setTitle(truncateTitle(chatRequest.getUserPrompt()));
            chatSession.setModelName(modelName);
            chatSessionService.save(chatSession);
        }

        // 当前序列
        Integer currentSeq = chatMessageService.getCurrentSeq(chatRequest.getSessionId());

        // 用户发送的消息
        ChatMessage userMessage = new ChatMessage();
        userMessage.setId(UuidUtil.get32UUID());
        userMessage.setSessionId(chatRequest.getSessionId());
        userMessage.setRole(MessageRoleEnum.USER);
        userMessage.setContent(chatRequest.getUserPrompt());
        userMessage.setSequence(currentSeq + 1);
        // 保存用户消息
        chatMessageService.save(userMessage);

        ChatCompletionCreateParams params = buildChatParams(chatRequest.getUserPrompt());
        List<ChatCompletionChunk> chunks = llmService.execute(params);

        // 模型返回的消息
        ChatMessage modelMessage = new ChatMessage();
        modelMessage.setId(UuidUtil.get32UUID());
        modelMessage.setSessionId(chatRequest.getSessionId());
        modelMessage.setRole(MessageRoleEnum.ASSISTANT);
        modelMessage.setSequence(currentSeq + 2);

        // 累加拼接流式内容
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
            chunk.usage().ifPresent(usage -> {
                modelMessage.setPromptTokenUsage(usage.promptTokens());
                modelMessage.setCompletionTokenUsage(usage.completionTokens());
                modelMessage.setTotalTokenUsage(usage.totalTokens());
            });

        }

        modelMessage.setContent(contentSb.toString());
        modelMessage.setReasoning(reasonSb.toString());
        // 保存模型消息
        chatMessageService.save(modelMessage);

        ChatResponseDTO chatResponse = new ChatResponseDTO();
        chatResponse.setContent(contentSb.toString());
        chatResponse.setReasoning(reasonSb.toString());

        return chatResponse;
    }

    @Override
    public Flux<ChatResponseDTO> streamChat(ChatRequestDTO chatRequest) {
        boolean exists = chatSessionService.lambdaQuery()
                .eq(ChatSession::getId, chatRequest.getSessionId())
                .exists();
        if (!exists) {
            // 是会话的第一条消息，创建新会话
            ChatSession chatSession = new ChatSession();
            chatSession.setId(chatRequest.getSessionId());
            chatSession.setTitle(truncateTitle(chatRequest.getUserPrompt()));
            chatSession.setModelName(modelName);
            chatSessionService.save(chatSession);
        }

        // 当前序列
        Integer currentSeq = chatMessageService.getCurrentSeq(chatRequest.getSessionId());

        // 用户发送的消息
        ChatMessage userMessage = new ChatMessage();
        userMessage.setId(UuidUtil.get32UUID());
        userMessage.setSessionId(chatRequest.getSessionId());
        userMessage.setRole(MessageRoleEnum.USER);
        userMessage.setContent(chatRequest.getUserPrompt());
        userMessage.setSequence(currentSeq + 1);
        // 保存用户消息
        chatMessageService.save(userMessage);

        ChatCompletionCreateParams params = buildChatParams(chatRequest.getUserPrompt());

        // 模型返回的消息
        ChatMessage modelMessage = new ChatMessage();
        modelMessage.setId(UuidUtil.get32UUID());
        modelMessage.setSessionId(chatRequest.getSessionId());
        modelMessage.setRole(MessageRoleEnum.ASSISTANT);
        modelMessage.setSequence(currentSeq + 2);
        StringBuilder contentSb = new StringBuilder();
        StringBuilder reasonSb = new StringBuilder();

        return llmService.executeStreaming(params)
                .doOnNext(chunk -> {
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
                    chunk.usage().ifPresent(usage -> {
                        modelMessage.setPromptTokenUsage(usage.promptTokens());
                        modelMessage.setCompletionTokenUsage(usage.completionTokens());
                        modelMessage.setTotalTokenUsage(usage.totalTokens());
                    });
                })
                // 转为ChatResponse
                .map(this::chunkToChatResponse)
                // 过滤掉没有内容的消息
                .filter(dto -> dto.getContent() != null || dto.getReasoning() != null)
                // 保存模型消息
                .doOnComplete(() -> {
                    modelMessage.setContent(contentSb.toString());
                    modelMessage.setReasoning(reasonSb.toString());
                    chatMessageService.save(modelMessage);
                });
    }

    private String truncateTitle(String content) {
        if (content == null || content.isBlank()) {
            return "新对话";
        }

        String cleanContent = content.trim().replaceAll("\\s+", " ");

        int maxLength = 15;
        if (cleanContent.length() <= maxLength) {
            return cleanContent;
        }

        return cleanContent.substring(0, maxLength) + "...";
    }

    private ChatCompletionCreateParams buildChatParams(String prompt) {

        return ChatCompletionCreateParams.builder()
                .addUserMessage(prompt)
                .model(modelName)
                .streamOptions(ChatCompletionStreamOptions.builder()
                        .includeUsage(true)
                        .build())
                .build();
    }

    private ChatResponseDTO chunkToChatResponse(ChatCompletionChunk chunk) {
        ChatResponseDTO chatResponse = new ChatResponseDTO();
        chunk.choices().stream().findFirst().ifPresent(choice -> {
            choice.delta().content().ifPresent(content -> {
                if (StringUtils.isNotBlank(content)) {
                    chatResponse.setContent(content);
                }
            });
            Optional.of(choice.delta()._additionalProperties()).ifPresent(additionalProperties -> {
                if (additionalProperties.containsKey("reasoning_content") && !additionalProperties.get("reasoning_content").isNull()) {
                    chatResponse.setReasoning(additionalProperties.get("reasoning_content").toString());
                }
            });
        });
        return chatResponse;
    }
}
