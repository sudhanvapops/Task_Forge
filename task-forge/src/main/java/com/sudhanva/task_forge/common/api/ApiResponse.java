package com.sudhanva.task_forge.common.api;


public record ApiResponse<T>(
    boolean success,
    String message,
    T data
) {}
