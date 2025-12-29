package org.example.aiagent.dto;

import lombok.Data;

@Data
public class CommonResponse<T> {
    private Integer code;
    private String message;
    private T data;

    protected CommonResponse() {
    }

    protected CommonResponse(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> CommonResponse<T> success() {
        return new CommonResponse<>(200, "success", null);
    }

    public static <T> CommonResponse<T> success(T data) {
        return new CommonResponse<>(200, "success", data);
    }

    public static <T> CommonResponse<T> failed(String message) {
        return new CommonResponse<>(500, message, null);
    }
}
