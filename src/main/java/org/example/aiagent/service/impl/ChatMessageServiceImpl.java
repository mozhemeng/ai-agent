package org.example.aiagent.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.aiagent.entity.ChatMessage;
import org.example.aiagent.mapper.ChatMessageMapper;
import org.example.aiagent.service.ChatMessageService;
import org.springframework.stereotype.Service;

@Service
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage> implements ChatMessageService {
    @Override
    public Integer getCurrentSeq(String sessionId) {
        Long count = this.lambdaQuery().eq(ChatMessage::getSessionId, sessionId).count();
        return count.intValue();
    }
}
