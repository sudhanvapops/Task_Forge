package com.sudhanva.task_forge.job.exception_handlers;


import java.time.Instant;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.sudhanva.task_forge.common.api.ErrorCode;
import com.sudhanva.task_forge.common.api.ErrorResponseDto;
import com.sudhanva.task_forge.job.exceptions.DuplicateJobException;
import com.sudhanva.task_forge.job.exceptions.ExecutorShutdownException;
import com.sudhanva.task_forge.job.exceptions.InvalidPayloadException;
import com.sudhanva.task_forge.job.exceptions.JobNotFoundException;
import com.sudhanva.task_forge.job.exceptions.QueueFullException;

@RestControllerAdvice
@Order(1)
public class JobExceptionHandler {

    @ExceptionHandler(DuplicateJobException.class)
    public ResponseEntity<ErrorResponseDto> handleDuplicatejob(
        DuplicateJobException exception
    ){
        ErrorResponseDto response = ErrorResponseDto.builder()
            .success(false)
            .errorCode(ErrorCode.CONFLICT)
            .message(exception.getMessage())
            .timestamp(Instant.now())
            .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler
     public ResponseEntity<ErrorResponseDto> jobNotFound(
        JobNotFoundException exception
    ){
        ErrorResponseDto response = ErrorResponseDto.builder()
            .success(false)
            .errorCode(ErrorCode.JOB_NOT_FOUND)
            .message(exception.getMessage())
            .timestamp(Instant.now())
            .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }


    @ExceptionHandler
     public ResponseEntity<ErrorResponseDto> handleQueueFull(
        QueueFullException exception
    ){
        ErrorResponseDto response = ErrorResponseDto.builder()
            .success(false)
            .errorCode(ErrorCode.QUEUE_FULL)
            .message(exception.getMessage())
            .timestamp(Instant.now())
            .build();

        // 503
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @ExceptionHandler
     public ResponseEntity<ErrorResponseDto> handleExecutorShutdown(
        ExecutorShutdownException exception
    ){
        ErrorResponseDto response = ErrorResponseDto.builder()
            .success(false)
            .errorCode(ErrorCode.EXECUTOR_SHUTDOWN)
            .message(exception.getMessage())
            .timestamp(Instant.now())
            .build();

        // 503
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @ExceptionHandler(InvalidPayloadException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidPayload(InvalidPayloadException exception) {
        ErrorResponseDto response = ErrorResponseDto.builder()
            .success(false)
            .errorCode(ErrorCode.INVALID_PAYLOAD)
            .message(exception.getMessage())
            .timestamp(Instant.now())
            .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }


}
