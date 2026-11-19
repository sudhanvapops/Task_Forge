package com.sudhanva.task_forge.job.exceptions;

import java.util.UUID;

public class DuplicateJobException extends JobException{
    public DuplicateJobException(UUID id) {
        super("Job already exists: " + id);
    }
}
