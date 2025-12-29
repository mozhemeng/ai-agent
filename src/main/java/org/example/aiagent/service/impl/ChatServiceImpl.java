package org.example.aiagent.service.impl;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.example.aiagent.dto.ChatRequestDTO;
import org.example.aiagent.dto.ChatResponseDTO;
import org.example.aiagent.entity.ChatMessage;
import org.example.aiagent.entity.ChatSession;
import org.example.aiagent.entity.ModelInput;
import org.example.aiagent.entity.ModelOutput;
import org.example.aiagent.enums.MessageRoleEnum;
import org.example.aiagent.service.ChatMessageService;
import org.example.aiagent.service.ChatService;
import org.example.aiagent.service.ChatSessionService;
import org.example.aiagent.service.LlmService;
import org.example.aiagent.util.UuidUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Comparator;
import java.util.List;


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

        // 最近的10条历史对话
        List<ChatMessage> messageList = chatMessageService.lambdaQuery()
                .eq(ChatMessage::getSessionId, chatRequest.getSessionId())
                .isNotNull(ChatMessage::getSequence)
                .orderByDesc(ChatMessage::getSequence)
                .last("limit 10")
                .list();

        // 当前序列
        int currentSeq = messageList.stream().map(ChatMessage::getSequence).max(Integer::compareTo).orElse(0);

        // 重新按顺序排序
        messageList.sort(Comparator.comparingInt(ChatMessage::getSequence));

        // 用户发送的消息
        ChatMessage userMessage = new ChatMessage();
        userMessage.setId(UuidUtil.get32UUID());
        userMessage.setSessionId(chatRequest.getSessionId());
        userMessage.setRole(MessageRoleEnum.USER);
        userMessage.setContent(chatRequest.getUserPrompt());
        userMessage.setSequence(currentSeq + 1);
        // 保存用户消息
        chatMessageService.save(userMessage);

        // 构造模型入参
        ModelInput input = new ModelInput();
        input.setUserPrompt(chatRequest.getUserPrompt());
        input.setHistoryChatMessageList(messageList);
        List<ModelOutput> outputList = llmService.execute(input);

        // 模型返回的消息
        ChatMessage modelMessage = new ChatMessage();
        modelMessage.setId(UuidUtil.get32UUID());
        modelMessage.setSessionId(chatRequest.getSessionId());
        modelMessage.setRole(MessageRoleEnum.ASSISTANT);
        modelMessage.setSequence(currentSeq + 2);

        // 累加拼接流式内容
        StringBuilder contentSb = new StringBuilder();
        StringBuilder reasonSb = new StringBuilder();
        for (ModelOutput output : outputList) {
            if (StringUtils.isNotEmpty(output.getContent())) {
                contentSb.append(output.getContent());
            }
            if (StringUtils.isNotEmpty(output.getReasoning())) {
                reasonSb.append(output.getReasoning());
            }
            if (output.getUsage() != null) {
                modelMessage.setPromptTokenUsage(output.getUsage().getPromptTokens());
                modelMessage.setCompletionTokenUsage(output.getUsage().getCompletionTokens());
                modelMessage.setTotalTokenUsage(output.getUsage().getTotalTokens());
            }
        }

        // 保存模型消息
        modelMessage.setContent(contentSb.toString());
        modelMessage.setReasoning(reasonSb.toString());
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

        // 最近的10条历史对话
        List<ChatMessage> messageList = chatMessageService.lambdaQuery()
                .eq(ChatMessage::getSessionId, chatRequest.getSessionId())
                .isNotNull(ChatMessage::getSequence)
                .orderByDesc(ChatMessage::getSequence)
                .last("limit 10")
                .list();

        // 当前序列
        int currentSeq = messageList.stream().map(ChatMessage::getSequence).max(Integer::compareTo).orElse(0);

        // 重新按顺序排序
        messageList.sort(Comparator.comparingInt(ChatMessage::getSequence));

        // 用户发送的消息
        ChatMessage userMessage = new ChatMessage();
        userMessage.setId(UuidUtil.get32UUID());
        userMessage.setSessionId(chatRequest.getSessionId());
        userMessage.setRole(MessageRoleEnum.USER);
        userMessage.setContent(chatRequest.getUserPrompt());
        userMessage.setSequence(currentSeq + 1);
        // 保存用户消息
        chatMessageService.save(userMessage);

        // 构造模型入参
        ModelInput input = new ModelInput();
        input.setHistoryChatMessageList(messageList);
        input.setUserPrompt(chatRequest.getUserPrompt());

        // 模型返回的消息
        ChatMessage modelMessage = new ChatMessage();
        modelMessage.setId(UuidUtil.get32UUID());
        modelMessage.setSessionId(chatRequest.getSessionId());
        modelMessage.setRole(MessageRoleEnum.ASSISTANT);
        modelMessage.setSequence(currentSeq + 2);
        StringBuilder contentSb = new StringBuilder();
        StringBuilder reasonSb = new StringBuilder();

        return llmService.executeStreaming(input)
                .doOnNext(output -> {
                    if (StringUtils.isNotEmpty(output.getContent())) {
                        contentSb.append(output.getContent());
                    }
                    if (StringUtils.isNotEmpty(output.getReasoning())) {
                        reasonSb.append(output.getReasoning());
                    }
                    if (output.getUsage() != null) {
                        modelMessage.setPromptTokenUsage(output.getUsage().getPromptTokens());
                        modelMessage.setCompletionTokenUsage(output.getUsage().getCompletionTokens());
                        modelMessage.setTotalTokenUsage(output.getUsage().getTotalTokens());
                    }
                })
                // 转为ChatResponse
                .map(output -> {
                    ChatResponseDTO chatResponse = new ChatResponseDTO();
                    chatResponse.setContent(output.getContent());
                    chatResponse.setReasoning(output.getReasoning());
                    return chatResponse;
                })
                // 过滤掉没有内容的消息
                .filter(dto -> StringUtils.isNotEmpty(dto.getContent()) || StringUtils.isNotEmpty(dto.getReasoning()))
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
}
