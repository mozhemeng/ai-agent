package org.example.aiagent.service.impl;

import com.openai.client.OpenAIClient;
import com.openai.core.JsonValue;
import com.openai.core.http.AsyncStreamResponse;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.example.aiagent.dto.LlmResponseDTO;
import org.example.aiagent.service.LlmService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class LlmServiceImpl implements LlmService {

    @Value("${llm.model-name}")
    private String modelName;

    private final OpenAIClient openAIClient;

    @Override
    public LlmResponseDTO chat(String prompt) {
        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .addUserMessage(prompt)
                .model(modelName)
                // 非流式不能开启思考内容
                .putAdditionalBodyProperty("enable_thinking", JsonValue.from(false))
                .build();

        CompletableFuture<ChatCompletion> future = openAIClient.async().chat().completions().create(params);
        ChatCompletion chatCompletion = future.join();

        LlmResponseDTO llmResponse = new LlmResponseDTO();
        if (!chatCompletion.isValid()) {
            log.warn("有问题的回复: {}", chatCompletion);
            return llmResponse;
        }

        ChatCompletion.Choice choice = chatCompletion.choices().get(0);
        choice.message().content().ifPresent(content -> {
            if (StringUtils.isNotBlank(content)) {
                llmResponse.setContent(content);
            }
        });

        return llmResponse;
    }

    @Override
    public Flux<LlmResponseDTO> streamChat(String prompt) {
        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .addUserMessage(prompt)
                .model(modelName)
                .build();

        AsyncStreamResponse<ChatCompletionChunk> streamResponse =
                openAIClient.async().chat().completions().createStreaming(params);

        AtomicBoolean isFinished = new AtomicBoolean(false);

        return Flux.create(sink -> {
            streamResponse.subscribe(chunk -> {
                        if (isFinished.get()) {
                            log.info("流已结束");
                            return;
                        }
                        if (!chunk.isValid()) {
                            log.warn("流中出现有问题的chunk: {}， 跳过", chunk);
                            return;
                        }
                        chunk.choices().stream().findFirst().ifPresent(choice -> {
                            LlmResponseDTO llmResponse = new LlmResponseDTO();
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
                            if (llmResponse.getContent() != null || llmResponse.getReasoning() != null) {
                                sink.next(llmResponse);
                            }
                            choice.finishReason().ifPresent(reason -> {
                                log.info("流结束，原因: {}", reason);
                                isFinished.set(true);
                                sink.complete();
                            });
                        });
                    }
            );
            streamResponse.onCompleteFuture().whenComplete((unused, throwable) -> {
                if (throwable != null) {
                    sink.error(throwable);
                } else {
                    sink.complete();
                }
            });
            sink.onCancel(() -> {
                log.info("前端断开了链接");
                isFinished.set(true);
            });
        });
    }
}
