package org.example.aiagent.service;

import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import reactor.core.publisher.Flux;

import java.util.List;

public interface LlmService {
    List<ChatCompletionChunk> execute(ChatCompletionCreateParams params);

    Flux<ChatCompletionChunk> executeStreaming(ChatCompletionCreateParams params);
}
