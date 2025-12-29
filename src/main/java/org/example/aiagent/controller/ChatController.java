package org.example.aiagent.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.aiagent.dto.ChatRequestDTO;
import org.example.aiagent.dto.ChatResponseDTO;
import org.example.aiagent.service.ChatService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/hello")
    public String hello() {
        return "Hello, World!";
    }

    @PostMapping("/chat")
    public ChatResponseDTO chat(@RequestBody @Valid ChatRequestDTO requestDTO) {
        return chatService.chat(requestDTO);
    }

    @PostMapping(value = "/stream-chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatResponseDTO> stream(@RequestBody @Valid ChatRequestDTO requestDTO) {
        return chatService.streamChat(requestDTO);
    }
}
