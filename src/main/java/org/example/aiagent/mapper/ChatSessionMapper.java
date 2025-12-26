package org.example.aiagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.aiagent.entity.ChatSession;

@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {
}
