package org.example.aiagent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import org.example.aiagent.enums.MessageRoleEnum;

import java.time.LocalDateTime;

@Data
@TableName("chat_message")
public class ChatMessage {
    @TableId(type = IdType.INPUT)
    private String id;
    private String sessionId;
    private MessageRoleEnum role;
    private String content;
    private String reasoning;
    private String attachment;
    private Long promptTokenUsage;
    private Long completionTokenUsage;
    private Long totalTokenUsage;
    private Integer sequence;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
