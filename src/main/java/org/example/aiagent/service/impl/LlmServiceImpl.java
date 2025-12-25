package org.example.aiagent.service.impl;

import com.openai.client.OpenAIClient;
import com.openai.core.JsonValue;
import com.openai.core.http.AsyncStreamResponse;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import lombok.RequiredArgsConstructor;
import org.example.aiagent.dto.LlmResponseDTO;
import org.example.aiagent.service.LlmService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class LlmServiceImpl implements LlmService {

    @Value("${llm.model-name}")
    private String modelName;

    private final OpenAIClient openAIClient;

    @Override
    public LlmResponseDTO chat(String prompt) {
        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .addUserMessage(prompt)
                .model(modelName)
                .putAdditionalBodyProperty("enable_thinking", JsonValue.from(false))
                .build();

        CompletableFuture<ChatCompletion> future = openAIClient.async().chat().completions().create(params);
        ChatCompletion chatCompletion = future.join();
        if (!chatCompletion.isValid()) {
            return null;
        }

        LlmResponseDTO llmResponse = new LlmResponseDTO();
        ChatCompletion.Choice choice = chatCompletion.choices().get(0);
        if (choice.message().content().isPresent()) {
            llmResponse.setContent(choice.message().content().get());
        }

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

        return Flux.create(sink -> {
            streamResponse.subscribe(chunk -> {
                        if (!chunk.isValid()) {
                            System.out.println("有问题的chunk: " + chunk);
                        }
                        chunk.choices().forEach(choice -> {
                            LlmResponseDTO llmResponse = new LlmResponseDTO();
                            if (choice.delta().content().isPresent()) {
                                llmResponse.setContent(choice.delta().content().get());
                            }
                            if (choice.delta()._additionalProperties().containsKey("reasoning_content")) {
                                llmResponse.setReasoning(choice.delta()._additionalProperties().get("reasoning_content").toString());
                            }
                            sink.next(llmResponse);
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
            sink.onCancel(streamResponse::close);
        });
    }
}
