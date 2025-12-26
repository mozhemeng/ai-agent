package org.example.aiagent.util;

import java.util.UUID;

public class UuidUtil {
    public static String get32UUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static void main(String[] args) {
        System.out.println(get32UUID());
    }
}
