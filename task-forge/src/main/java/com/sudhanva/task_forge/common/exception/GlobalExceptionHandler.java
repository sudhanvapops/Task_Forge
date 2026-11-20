package com.sudhanva.task_forge.common.exception;

import java.time.Instant;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.sudhanva.task_forge.common.api.ErrorCode;
import com.sudhanva.task_forge.common.api.ErrorResponseDto;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> globalHandler(
        Exception exception
    ){
        ErrorResponseDto response = ErrorResponseDto.builder()
            .success(false)
            .errorCode(ErrorCode.INTERNAL_SERVER_ERROR)
            .message(exception.getMessage())
            .timestamp(Instant.now())
            .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidJson(
        HttpMessageNotReadableException exception
    ) {

        ErrorResponseDto response = ErrorResponseDto.builder()
            .success(false)
            .errorCode(ErrorCode.BAD_REQUEST)
            .message(exception.getMessage())
            .timestamp(Instant.now())
            .build();
        return ResponseEntity.badRequest().body(response);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidation(
        MethodArgumentNotValidException exception
    ) {

        ErrorResponseDto response = ErrorResponseDto.builder()
            .success(false)
            .errorCode(ErrorCode.BAD_REQUEST)
            .message(exception.getMessage())
            .timestamp(Instant.now())
            .build();
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDto> handleTypeMismatch(
        MethodArgumentTypeMismatchException exception
    ) {

        ErrorResponseDto response = ErrorResponseDto.builder()
            .success(false)
            .errorCode(ErrorCode.BAD_REQUEST)
            .message(exception.getMessage())
            .timestamp(Instant.now())
            .build();
        return ResponseEntity.badRequest().body(response);
    }
    
}
