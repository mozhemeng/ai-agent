package org.example.aiagent.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.aiagent.entity.ChatMessage;

public interface ChatMessageService extends IService<ChatMessage> {

    Integer getCurrentSeq(String sessionId);
}
