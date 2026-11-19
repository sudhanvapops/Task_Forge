package com.sudhanva.task_forge.job.exceptions;

import java.util.UUID;

public class DuplicateJobException extends RuntimeException{
    public DuplicateJobException(UUID id) {
        super("Job already exists: " + id);
    }
}
