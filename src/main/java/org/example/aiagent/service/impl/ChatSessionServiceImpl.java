package org.example.aiagent.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.aiagent.entity.ChatSession;
import org.example.aiagent.mapper.ChatSessionMapper;
import org.example.aiagent.service.ChatSessionService;
import org.springframework.stereotype.Service;

@Service
public class ChatSessionServiceImpl extends ServiceImpl<ChatSessionMapper, ChatSession> implements ChatSessionService {
}
