package com.sudhanva.task_forge.job.exceptions;

public class InvalidPayloadException extends JobException {
    public InvalidPayloadException(String message) { super(message); }
    public InvalidPayloadException(String message, Throwable cause) { super(message, cause); }
}