package com.wk.ti.util;

import org.springframework.http.HttpHeaders;

import java.util.stream.Collectors;

public class HeaderUtil {

    private static final java.util.Set<String> SENSITIVE_HEADERS = java.util.Set.of(
            "X-APIKey",
            "Authorization",
            "Cookie",
            "Set-Cookie",
            "X-API-Key",
            "X-Auth-Token"
    );

    public static String maskSensitiveHeaders(HttpHeaders headers) {
        return headers.entrySet().stream()
                .map(entry -> {
                    String headerName = entry.getKey();
                    boolean isSensitive = SENSITIVE_HEADERS.stream()
                            .anyMatch(s -> s.equalsIgnoreCase(headerName));
                    if (isSensitive) {
                        return headerName + ":\"[MASKED]\"";
                    }
                    return headerName + ":\"" + entry.getValue() + "\"";
                })
                .collect(Collectors.joining(", ", "[", "]"));
    }
}
