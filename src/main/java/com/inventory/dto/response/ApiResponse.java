package com.inventory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.MDC;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private String error;
    private String errorCode;
    private String correlationId;
    private Instant timestamp;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message("Success")
                .data(data)
                .correlationId(MDC.get("traceId"))
                .timestamp(Instant.now())
                .build();
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .correlationId(MDC.get("traceId"))
                .timestamp(Instant.now())
                .build();
    }

    public static <T> ApiResponse<T> error(String error, String errorCode) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(error)
                .errorCode(errorCode)
                .correlationId(MDC.get("traceId"))
                .timestamp(Instant.now())
                .build();
    }
}
