package com.sudhanva.task_forge.common.api;

import java.time.LocalDateTime;

import lombok.Builder;

@Builder
public record ErrorResponseDto(
    boolean success,
    ErrorCode errorCode,
    String message,
    LocalDateTime timestamp
) {
    
}
