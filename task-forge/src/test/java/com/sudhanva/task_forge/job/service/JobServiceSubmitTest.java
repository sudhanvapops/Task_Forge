
package com.sudhanva.task_forge.job.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import com.sudhanva.task_forge.job.api.dto.SubmitJobRequest;
import com.sudhanva.task_forge.job.exceptions.ExecutorShutdownException;
import com.sudhanva.task_forge.job.exceptions.InvalidPayloadException;
import com.sudhanva.task_forge.job.exceptions.QueueFullException;
import com.sudhanva.task_forge.job.handler.EchoPayload;
import com.sudhanva.task_forge.job.interfaces.JobHandler;
import com.sudhanva.task_forge.job.interfaces.JobHandlerRegistry;
import com.sudhanva.task_forge.job.interfaces.JobType;
import com.sudhanva.task_forge.job.model.Job;
import com.sudhanva.task_forge.job.model.enums.JobStatus;
import com.sudhanva.task_forge.job.store.InMemoryJobStore;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

// Plain JUnit + Mockito. No Spring context, no database.
// REAL: store, thread pool, JSON mapper, validator.
// FAKE: JobHandler, so each test controls job execution.

class JobServiceSubmitTest {

    private static final int WAIT_SECONDS = 5;

    private final JsonMapper mapper = JsonMapper.builder().build();

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    private InMemoryJobStore store;
    private FakeHandler handler;
    private JobService service;

    // Spring executor and its underlying JDK executor.
    private ThreadPoolTaskExecutor executor;
    private ThreadPoolExecutor pool;

    @BeforeEach
    void setUp() {
        store = spy(new InMemoryJobStore());
        handler = new FakeHandler();

        executor = buildPool(2, 2, 10);
        pool = executor.getThreadPoolExecutor();

        service = newService(
                executor,
                new JobHandlerRegistry(List.of(handler))
        );
    }

    @AfterEach
    void tearDown() {
        // Never leave a worker blocked.
        handler.gate.countDown();

        if (pool != null) {
            pool.shutdownNow();
        }
    }

    // ==========================================================
    // Helpers
    // ==========================================================

    private ThreadPoolTaskExecutor buildPool(
            int core,
            int max,
            int queueSize) {

        ThreadPoolTaskExecutor e = new ThreadPoolTaskExecutor();

        e.setCorePoolSize(core);
        e.setMaxPoolSize(max);
        e.setQueueCapacity(queueSize);

        e.setRejectedExecutionHandler(
                new ThreadPoolExecutor.AbortPolicy()
        );

        e.initialize();

        return e;
    }

    // Constructor order must match JobService fields.
    private JobService newService(
            ThreadPoolTaskExecutor executor,
            JobHandlerRegistry registry) {

        return new JobService(
                store,
                executor,
                registry,
                mapper,
                validator
        );
    }

    // Replace the default pool with a smaller pool.
    private void useSmallPool(
            int core,
            int max,
            int queueSize) {

        pool.shutdownNow();

        executor = buildPool(core, max, queueSize);
        pool = executor.getThreadPoolExecutor();

        service = newService(
                executor,
                new JobHandlerRegistry(List.of(handler))
        );
    }

    private SubmitJobRequest echo(String message, long delayMs) {
        return requestWith(
                "{\"message\":\"" + message
                        + "\",\"delayMs\":" + delayMs + "}"
        );
    }

    private SubmitJobRequest requestWith(String payloadJson) {
        try {
            JsonNode payload = mapper.readTree(payloadJson);

            return new SubmitJobRequest(
                    JobType.ECHO,
                    payload
            );

        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Invalid test JSON",
                    e
            );
        }
    }

    // Open the gate and wait until accepted jobs finish.
    private void finishAllJobs() throws Exception {
        handler.gate.countDown();

        pool.shutdown();

        assertTrue(
                pool.awaitTermination(WAIT_SECONDS, TimeUnit.SECONDS),
                "Jobs did not finish in time"
        );
    }

    // ==========================================================
    // submit(): Happy path
    // ==========================================================

    @Test
    void submit_validRequest_returnsQueuedJobWithIdAndType() {

        Job job = service.submit(echo("hi", 0));

        assertNotNull(job.id());
        assertEquals(JobStatus.QUEUED, job.status());
        assertEquals(JobType.ECHO, job.type());
    }

    @Test
    void submit_validRequest_jobIsInTheStore() {

        Job job = service.submit(echo("hi", 0));

        assertDoesNotThrow(
                () -> service.getJob(job.id())
        );
    }

    @Test
    void submit_twice_givesTwoDifferentIds() {

        Job a = service.submit(echo("a", 0));
        Job b = service.submit(echo("b", 0));

        assertNotEquals(a.id(), b.id());
    }

    @Test
    void submit_handlerReceivesTheConvertedPayload() throws Exception {

        service.submit(echo("hello", 25));

        finishAllJobs();

        assertEquals(1, handler.received.size());
        assertEquals("hello", handler.received.get(0).message());
        assertEquals(25, handler.received.get(0).delayMs());
    }

    // ==========================================================
    // submit(): Invalid input
    // Must fail BEFORE any state change.
    // ==========================================================

    @Test
    void submit_payloadWithWrongShape_throwsInvalidPayload() {

        assertThrows(
                InvalidPayloadException.class,
                () -> service.submit(requestWith("{\"x\":1}"))
        );
    }

    @Test
    void submit_blankMessage_throwsInvalidPayload() {

        assertThrows(
                InvalidPayloadException.class,
                () -> service.submit(
                        requestWith("{\"message\":\"  \",\"delayMs\":0}")
                )
        );
    }

    @Test
    void submit_delayTooBig_throwsInvalidPayload() {

        assertThrows(
                InvalidPayloadException.class,
                () -> service.submit(
                        requestWith("{\"message\":\"hi\",\"delayMs\":9001}")
                )
        );
    }

    @Test
    void submit_negativeDelay_throwsInvalidPayload() {

        assertThrows(
                InvalidPayloadException.class,
                () -> service.submit(
                        requestWith("{\"message\":\"hi\",\"delayMs\":-1}")
                )
        );
    }

    @Test
    void submit_nullPayload_throwsInvalidPayload() {

        assertThrows(
                InvalidPayloadException.class,
                () -> service.submit(requestWith("null"))
        );
    }

    @Test
    void submit_invalidPayload_savesNothingAndRunsNothing()
            throws Exception {

        assertThrows(
                InvalidPayloadException.class,
                () -> service.submit(requestWith("{\"x\":1}"))
        );

        verify(store, never()).save(any());

        finishAllJobs();

        assertEquals(0, handler.calls.get());
    }

    @Test
    void submit_typeWithNoRegisteredHandler_throwsIllegalArgument() {

        JobService noHandlers = newService(
                executor,
                new JobHandlerRegistry(List.of())
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> noHandlers.submit(echo("hi", 0))
        );

        verify(store, never()).save(any());
    }

    // ==========================================================
    // submit(): Pool full or shut down
    // ==========================================================

    @Test
    void submit_poolAndQueueFull_throwsQueueFull() throws Exception {

        useSmallPool(1, 1, 1);

        handler.gate = new CountDownLatch(1);

        // First job occupies the only worker.
        service.submit(echo("running", 0));

        assertTrue(
                handler.firstStarted.await(
                        WAIT_SECONDS,
                        TimeUnit.SECONDS
                )
        );

        // Second job occupies the only queue slot.
        service.submit(echo("queued", 0));

        // Third job must be rejected.
        assertThrows(
                QueueFullException.class,
                () -> service.submit(echo("rejected", 0))
        );
    }

    @Test
    void submit_queueFull_removesTheRejectedJobFromTheStore()
            throws Exception {

        useSmallPool(1, 1, 1);

        handler.gate = new CountDownLatch(1);

        service.submit(echo("running", 0));

        assertTrue(
                handler.firstStarted.await(
                        WAIT_SECONDS,
                        TimeUnit.SECONDS
                )
        );

        service.submit(echo("queued", 0));

        assertThrows(
                QueueFullException.class,
                () -> service.submit(echo("rejected", 0))
        );

        // Three saves happened. The rejected job must be removed.
        ArgumentCaptor<Job> saved =
                ArgumentCaptor.forClass(Job.class);

        verify(store, times(3)).save(saved.capture());

        List<Job> jobs = saved.getAllValues();

        assertTrue(
                store.findById(jobs.get(0).id()).isPresent()
        );

        assertTrue(
                store.findById(jobs.get(1).id()).isPresent()
        );

        assertTrue(
                store.findById(jobs.get(2).id()).isEmpty()
        );
    }

    @Test
    void submit_executorShutDown_throwsExecutorShutdownNotQueueFull() {

        pool.shutdown();

        assertThrows(
                ExecutorShutdownException.class,
                () -> service.submit(echo("hi", 0))
        );
    }

    @Test
    void submit_executorShutDown_removesTheJobFromTheStore() {

        pool.shutdown();

        assertThrows(
                ExecutorShutdownException.class,
                () -> service.submit(echo("hi", 0))
        );

        ArgumentCaptor<Job> saved =
                ArgumentCaptor.forClass(Job.class);

        verify(store).save(saved.capture());

        assertTrue(
                store.findById(saved.getValue().id()).isEmpty()
        );
    }

    // ==========================================================
    // runJob(): Worker thread and status transitions
    // ==========================================================

    @Test
    void runJob_whileHandlerIsRunning_statusIsRunning()
            throws Exception {

        handler.gate = new CountDownLatch(1);

        Job job = service.submit(echo("hi", 0));

        assertTrue(
                handler.firstStarted.await(
                        WAIT_SECONDS,
                        TimeUnit.SECONDS
                )
        );

        assertEquals(
                JobStatus.RUNNING,
                service.getJob(job.id()).status()
        );

        finishAllJobs();
    }

    @Test
    void runJob_handlerSucceeds_statusBecomesSucceeded()
            throws Exception {

        Job job = service.submit(echo("hi", 0));

        finishAllJobs();

        assertEquals(
                JobStatus.SUCCEEDED,
                service.getJob(job.id()).status()
        );
    }

    @Test
    void runJob_handlerThrows_statusBecomesFailed()
            throws Exception {

        Job job = service.submit(
                echo(FakeHandler.FAIL_MESSAGE, 0)
        );

        finishAllJobs();

        assertEquals(
                JobStatus.FAILED,
                service.getJob(job.id()).status()
        );
    }

    @Test
    void runJob_afterAFailure_theWorkerStillRunsTheNextJob()
            throws Exception {

        // ONE worker thread: the same worker must survive the failure.
        useSmallPool(1, 1, 5);

        Job bad = service.submit(
                echo(FakeHandler.FAIL_MESSAGE, 0)
        );

        Job good = service.submit(
                echo("fine", 0)
        );

        finishAllJobs();

        assertEquals(
                JobStatus.FAILED,
                service.getJob(bad.id()).status()
        );

        assertEquals(
                JobStatus.SUCCEEDED,
                service.getJob(good.id()).status()
        );
    }

    @Test
    void runJob_jobRemovedBeforeItStarts_handlerNeverRunsAndNoGhostAppears()
            throws Exception {

        useSmallPool(1, 1, 5);

        handler.gate = new CountDownLatch(1);

        // Blocker occupies the only worker.
        service.submit(echo("blocker", 0));

        assertTrue(
                handler.firstStarted.await(
                        WAIT_SECONDS,
                        TimeUnit.SECONDS
                )
        );

        // This job waits in the queue.
        Job waiting = service.submit(echo("waiting", 0));

        // Remove it before a worker picks it up.
        store.remove(waiting.id());

        finishAllJobs();

        assertEquals(1, handler.calls.get());

        assertTrue(
                store.findById(waiting.id()).isEmpty()
        );
    }

    @Test
    void runJob_manyJobs_allEndSucceeded() throws Exception {

        useSmallPool(4, 4, 50);

        List<Job> jobs = new ArrayList<>();

        for (int i = 0; i < 40; i++) {
            jobs.add(
                    service.submit(echo("job-" + i, 0))
            );
        }

        finishAllJobs();

        assertEquals(40, handler.calls.get());

        for (Job job : jobs) {
            assertEquals(
                    JobStatus.SUCCEEDED,
                    service.getJob(job.id()).status()
            );
        }
    }

    // ==========================================================
    // Fake handler: fully controlled by the tests
    // ==========================================================

    static class FakeHandler
            implements JobHandler<EchoPayload, String> {

        static final String FAIL_MESSAGE = "bad";

        final AtomicInteger calls = new AtomicInteger();

        final List<EchoPayload> received =
                new CopyOnWriteArrayList<>();

        final CountDownLatch firstStarted =
                new CountDownLatch(1);

        // Open by default. Tests can replace this with a blocking latch.
        volatile CountDownLatch gate =
                new CountDownLatch(0);

        @Override
        public String execute(EchoPayload payload) throws Exception {

            calls.incrementAndGet();

            received.add(payload);

            firstStarted.countDown();

            // Wait until the test opens the gate.
            gate.await();

            if (FAIL_MESSAGE.equals(payload.message())) {
                throw new IllegalStateException("boom");
            }

            return "ok";
        }

        @Override
        public JobType type() {
            return JobType.ECHO;
        }

        @Override
        public Class<EchoPayload> payloadType() {
            return EchoPayload.class;
        }
    }
}
