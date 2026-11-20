package com.sudhanva.task_forge.job.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import java.util.UUID;
import java.util.concurrent.ThreadPoolExecutor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;
import com.sudhanva.task_forge.job.exceptions.JobNotFoundException;
import com.sudhanva.task_forge.job.interfaces.JobHandlerRegistry;
import com.sudhanva.task_forge.job.interfaces.JobType;
import com.sudhanva.task_forge.job.model.Job;
import com.sudhanva.task_forge.job.model.enums.JobStatus;
import com.sudhanva.task_forge.job.store.InMemoryJobStore;

import jakarta.validation.Validator;


class JobServiceGetJobTest {

    private InMemoryJobStore store;
    private JobService service;

    @BeforeEach
    void setUp() {
        store = new InMemoryJobStore();
        service = new JobService(
            store,
            mock(ThreadPoolExecutor.class),
            mock(JobHandlerRegistry.class),
            mock(ObjectMapper.class),
            mock(Validator.class)
        );
    }

    @Test
    void getJob_existingId_returnsThatJob() {
        Job job = new Job(UUID.randomUUID(), JobStatus.QUEUED, JobType.ECHO);
        store.save(job);

        assertEquals(job, service.getJob(job.id()));
    }

    @Test
    void getJob_reflectsTheLatestStatus() {
        Job job = new Job(UUID.randomUUID(), JobStatus.QUEUED, JobType.ECHO);
        store.save(job);
        store.updateStatus(job.id(), JobStatus.RUNNING);

        assertEquals(JobStatus.RUNNING, service.getJob(job.id()).status());
    }

    @Test
    void getJob_unknownId_throwsJobNotFoundException() {
        assertThrows(JobNotFoundException.class, () -> service.getJob(UUID.randomUUID()));
    }

    @Test
    void getJob_unknownId_messageContainsTheId() {
        UUID id = UUID.randomUUID();

        JobNotFoundException e = assertThrows(JobNotFoundException.class, () -> service.getJob(id));

        assertTrue(e.getMessage().contains(id.toString()));
    }

    @Test
    void getJob_afterRemove_throwsJobNotFoundException() {
        Job job = new Job(UUID.randomUUID(), JobStatus.QUEUED, JobType.ECHO);
        store.save(job);
        store.remove(job.id());

        assertThrows(JobNotFoundException.class, () -> service.getJob(job.id()));
    }
}