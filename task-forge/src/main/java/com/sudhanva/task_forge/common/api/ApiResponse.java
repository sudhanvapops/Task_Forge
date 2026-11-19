package com.sudhanva.task_forge.common.api;

public record ApiResponse<T>(
    boolean status,
    String message,
    T data
) {}
