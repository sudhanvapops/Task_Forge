package com.sudhanva.task_forge.job.service;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;
// import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import com.sudhanva.task_forge.job.api.dto.SubmitJobRequest;
import com.sudhanva.task_forge.job.exceptions.ExecutorShutdownException;
import com.sudhanva.task_forge.job.exceptions.InvalidPayloadException;
import com.sudhanva.task_forge.job.exceptions.JobNotFoundException;
import com.sudhanva.task_forge.job.exceptions.QueueFullException;
import com.sudhanva.task_forge.job.interfaces.JobHandler;
import com.sudhanva.task_forge.job.interfaces.JobHandlerRegistry;
import com.sudhanva.task_forge.job.interfaces.JobPayload;
import com.sudhanva.task_forge.job.model.Job;
import com.sudhanva.task_forge.job.model.enums.JobStatus;
import com.sudhanva.task_forge.job.store.JobStore;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

// Test These
// good:           {"type":"ECHO","payload":{"message":"hi"}}   -> 202
// wrong shape:    {"type":"ECHO","payload":{"x":1}}            -> 400 (not 500)
// unknown type:   {"type":"NOPE","payload":{}}                 -> 400
// missing payload:{"type":"ECHO"}                              -> 400

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobStore store;
    private final ThreadPoolTaskExecutor jobExecutor;
    private final JobHandlerRegistry registry;
    
    private final ObjectMapper objectMapper;
    private final Validator validator;

    private static final Logger log = LoggerFactory.getLogger(JobService.class);

    public Job submit(SubmitJobRequest request) {
        // checks that can fail come first
        // state changes last
        JobHandler<?, ?> handler = registry.getHandler(request.type());

        // convrting request payload to proper format
        JobPayload payload = toPayload(request.payload(), handler);

        Job job = new Job(UUID.randomUUID(), JobStatus.QUEUED, request.type());

        store.save(job);

        try {
            jobExecutor.execute(() -> runJob(job.id(), handler, payload));
        } catch (RejectedExecutionException e) {
            store.remove(job.id());
            throw jobExecutor.getThreadPoolExecutor().isShutdown()
                    ? new ExecutorShutdownException()
                    : new QueueFullException();
        } 
        return job;
    }

    // JSON -> payload object, then check its validation rules
    private JobPayload toPayload(JsonNode json, JobHandler<?, ?> handler) {
        JobPayload payload;

        try {
            payload = objectMapper.treeToValue(json, handler.payloadType());
        } catch (Exception e) {
            throw new InvalidPayloadException("Payload does not match the job type", e);
        }
        if (payload == null) {
            throw new InvalidPayloadException("Payload is empty");
        }

        // ? Not Understood
        Set<ConstraintViolation<JobPayload>> errors = validator.validate(payload);
        if (!errors.isEmpty()) {
            String message = errors.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .collect(Collectors.joining(", "));
            throw new InvalidPayloadException(message);
        }

        return payload;
    }

    // runs on the worker thread
    private void runJob(UUID id, JobHandler<?, ?> handler, JobPayload payload) {
        if (!setStatus(id, JobStatus.RUNNING)) {
            // job was removed, nothing to run
            return;
        }
        try {
            System.out.println(Thread.currentThread().getName());
            handler.executeUntyped(payload);
            setStatus(id, JobStatus.SUCCEEDED);
        } catch (Exception e) {
            log.error("Job {} failed", id, e);
            setStatus(id, JobStatus.FAILED);
        } catch (Throwable t) {
            // This is for Error crash
            // the ThreaPoolExecutor later 
            // will replaces the dyied worker
            // thats why throwing the error again to crash the worker thread
            log.error("Job {} failed", id, t);
            setStatus(id, JobStatus.FAILED);
            if (t instanceof Error) {
                throw t;
            }
        }
    }

    // one place for "update + log if the job is missing"
    private boolean setStatus(UUID id, JobStatus status) {
        boolean updated = store.updateStatus(id, status);
        if (!updated) {
            log.warn("Job {} not found when setting {}", id, status);
        }
        return updated;
    }

    public Job getJob(UUID id) {
        Job job = store.findById(id).orElseThrow(
                () -> new JobNotFoundException(id));
        return job;
    }

}
