package org.example.aiagent.service.impl;

import com.openai.client.OpenAIClient;
import com.openai.core.http.AsyncStreamResponse;
import com.openai.core.http.StreamResponse;
import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.aiagent.service.LlmService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LlmServiceImpl implements LlmService {

    private final OpenAIClient openAIClient;

    @Override
    public List<ChatCompletionChunk> execute(ChatCompletionCreateParams params) {
        List<ChatCompletionChunk> allChunks = new ArrayList<>();

        try (StreamResponse<ChatCompletionChunk> streamResponse = openAIClient.chat().completions().createStreaming(params)) {
            streamResponse.stream().forEach(chunk -> {
                if (chunk.isValid()) {
                    allChunks.add(chunk);
                }
            });
        }

        return allChunks;
    }

    @Override
    public Flux<ChatCompletionChunk> executeStreaming(ChatCompletionCreateParams params) {
        AsyncStreamResponse<ChatCompletionChunk> streamResponse =
                openAIClient.async().chat().completions().createStreaming(params);

        return Flux.create(sink -> {
            streamResponse.subscribe(chunk -> {
                if (chunk.isValid()) {
                    sink.next(chunk);
                }
                // 处理结束标识
                chunk.choices().stream()
                        .filter(c -> c.finishReason().isPresent())
                        .findFirst()
                        .ifPresent(c -> sink.complete());
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
}
