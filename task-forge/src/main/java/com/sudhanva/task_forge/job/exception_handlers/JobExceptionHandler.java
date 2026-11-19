package com.sudhanva.task_forge.job.exception_handlers;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.sudhanva.task_forge.common.api.ErrorCode;
import com.sudhanva.task_forge.common.api.ErrorResponseDto;
import com.sudhanva.task_forge.job.exceptions.DuplicateJobException;

@RestControllerAdvice
public class JobExceptionHandler {

    @ExceptionHandler(DuplicateJobException.class)
    public ResponseEntity<ErrorResponseDto> handleDuplicatejob(
        DuplicateJobException exception
    ){
        ErrorResponseDto response = ErrorResponseDto.builder()
            .success(false)
            .errorCode(ErrorCode.CONFLICT)
            .message(exception.getMessage())
            .timestamp(LocalDateTime.now())
            .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

}
