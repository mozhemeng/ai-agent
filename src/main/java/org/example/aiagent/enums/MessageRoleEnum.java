package org.example.aiagent.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MessageRoleEnum {
    SYSTEM("system", "系统指令"),
    USER("user", "终端用户"),
    ASSISTANT("assistant", "AI助手"),
    TOOL("tool", "工具/函数");

    @EnumValue
    @JsonValue
    private final String code;

    private final String name;
}
