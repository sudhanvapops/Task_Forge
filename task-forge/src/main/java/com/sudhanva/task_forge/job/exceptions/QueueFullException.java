package com.sudhanva.task_forge.job.exceptions;

public class QueueFullException extends JobException{
    public QueueFullException() {
        super("Queue is Full");
    }
}
