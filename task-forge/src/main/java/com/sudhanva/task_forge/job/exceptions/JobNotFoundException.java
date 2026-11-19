package com.sudhanva.task_forge.job.exceptions;

import java.util.UUID;

public class JobNotFoundException  extends JobException {
    public  JobNotFoundException(UUID id){
        super("Job not found: "+id);
    }
}
