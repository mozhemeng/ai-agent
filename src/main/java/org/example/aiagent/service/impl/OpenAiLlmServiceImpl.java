package org.example.aiagent.service.impl;

import com.openai.client.OpenAIClient;
import com.openai.core.http.AsyncStreamResponse;
import com.openai.core.http.StreamResponse;
import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionStreamOptions;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.example.aiagent.entity.ChatMessage;
import org.example.aiagent.entity.ModelInput;
import org.example.aiagent.entity.ModelOutput;
import org.example.aiagent.service.LlmService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenAiLlmServiceImpl implements LlmService {

    private final OpenAIClient openAIClient;

    @Value("${llm.model-name}")
    private String modelName;

    @Value("${llm.system-prompt-file}")
    private String systemPromptFile;

    private String systemPrompt;

    @PostConstruct
    public void init() {
        systemPrompt = getSystemPrompt();
    }

    public List<ModelOutput> execute(ModelInput input) {
        ChatCompletionCreateParams params = buildChatParams(input);

        List<ModelOutput> outputList = new ArrayList<>();
        try (StreamResponse<ChatCompletionChunk> streamResponse = openAIClient.chat().completions().createStreaming(params)) {
            streamResponse.stream().forEach(chunk -> {
                if (chunk != null) {
                    outputList.add(convertToModelOutput(chunk));
                }
            });
        }

        return outputList;
    }

    public Flux<ModelOutput> executeStreaming(ModelInput input) {
        ChatCompletionCreateParams params = buildChatParams(input);

        AsyncStreamResponse<ChatCompletionChunk> streamResponse =
                openAIClient.async().chat().completions().createStreaming(params);

        return Flux.create(sink -> {
            streamResponse.subscribe(chunk -> {
                if (chunk != null) {
                    sink.next(convertToModelOutput(chunk));
                }
            });

            streamResponse.onCompleteFuture().whenComplete((unused, throwable) -> {
                if (throwable != null) {
                    sink.error(throwable);
                } else {
                    sink.complete();
                }
            });

            sink.onCancel(() -> {
                log.info("上游断开了链接");
                streamResponse.close();
            });
        });
    }

    private ChatCompletionCreateParams buildChatParams(ModelInput input) {
        ChatCompletionCreateParams.Builder paramBuilder = ChatCompletionCreateParams.builder()
                .model(modelName)
                .streamOptions(ChatCompletionStreamOptions.builder()
                        .includeUsage(true)
                        .build())
                .addSystemMessage(systemPrompt);

        // 历史对话
        if (CollectionUtils.isNotEmpty(input.getHistoryChatMessageList())) {
            for (ChatMessage message : input.getHistoryChatMessageList()) {
                switch (message.getRole()) {
                    case USER:
                        paramBuilder.addUserMessage(message.getContent());
                        break;
                    case ASSISTANT:
                        paramBuilder.addAssistantMessage(message.getContent());
                        break;
                }
            }
        }

        // 当前prompt
        paramBuilder.addUserMessage(input.getUserPrompt());

        return paramBuilder.build();
    }

    private ModelOutput convertToModelOutput(ChatCompletionChunk chunk) {
        ModelOutput output = new ModelOutput();
        chunk.choices().stream().findFirst().ifPresent(choice -> {
            choice.delta().content().ifPresent(output::setContent);
            Optional.of(choice.delta()._additionalProperties()).ifPresent(additionalProperties -> {
                if (additionalProperties.containsKey("reasoning_content") && !additionalProperties.get("reasoning_content").isNull()) {
                    output.setReasoning(additionalProperties.get("reasoning_content").toString());
                }
            });
        });
        if (chunk.usage().isPresent()) {
            ModelOutput.Usage modelUsage = new ModelOutput.Usage();
            modelUsage.setPromptTokens(chunk.usage().get().promptTokens());
            modelUsage.setCompletionTokens(chunk.usage().get().completionTokens());
            modelUsage.setTotalTokens(chunk.usage().get().totalTokens());
            output.setUsage(modelUsage);
        }

        return output;
    }

    private String getSystemPrompt() {
        try {
            Resource resource = new ClassPathResource(systemPromptFile);
            String content = Files.readString(resource.getFile().toPath());
            log.info("读取到系统提示词：\n{}", content);
            return content;
        } catch (Exception e) {
            log.error("读取系统提示词失败", e);
            return "";
        }
    }
}
