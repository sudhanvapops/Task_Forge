package com.sudhanva.task_forge.job.exceptions;

public class ExecutorShutdownException extends JobException {
    public ExecutorShutdownException() {
        super("Executor Serivce is Shutdown");
    }
}
