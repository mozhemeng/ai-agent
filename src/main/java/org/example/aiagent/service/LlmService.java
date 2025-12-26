package org.example.aiagent.service;

import org.example.aiagent.entity.ModelInput;
import org.example.aiagent.entity.ModelOutput;
import reactor.core.publisher.Flux;

import java.util.List;

public interface LlmService {
    List<ModelOutput> execute(ModelInput input);

    Flux<ModelOutput> executeStreaming(ModelInput input);
}
