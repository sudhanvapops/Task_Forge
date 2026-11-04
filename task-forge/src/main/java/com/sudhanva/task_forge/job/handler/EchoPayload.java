package com.sudhanva.task_forge.job.handler;

import com.sudhanva.task_forge.job.JobPayload;

public record EchoPayload(
    String message,
    // in ms
    long delayMs // using primitive ssince it cannot be null
) implements JobPayload {

    public EchoPayload{

        if (message == null || message.isBlank()){
            throw new IllegalArgumentException("Message cannot be empty");
        }

        if (delayMs < 0 ){
            throw new IllegalArgumentException("delay must be positive");
        }
        
        if (delayMs > 9000){
            throw new IllegalArgumentException("delay must not exceed 9 seconds");
        }
    }

}
