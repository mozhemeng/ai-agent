package org.example.aiagent.controller;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.example.aiagent.dto.CommonResponse;
import org.example.aiagent.util.UuidUtil;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;

@RestController
@RequiredArgsConstructor
@RequestMapping("/helper")
public class HelperController {
    @GetMapping("/new-sessionId")
    public CommonResponse<String> getNewSessionId() {
        return CommonResponse.success(UuidUtil.get32UUID());
    }

    @PostMapping("/data-url/base64")
    public CommonResponse<String> getDataUrlWithBase64(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String mimeType = StringUtils.isNotBlank(file.getContentType()) ? file.getContentType() : "application/octet-stream";

        String base64 = Base64.getEncoder().encodeToString(file.getBytes());

        return CommonResponse.success(String.format("data:%s;base64,%s", mimeType, base64));
    }
}
