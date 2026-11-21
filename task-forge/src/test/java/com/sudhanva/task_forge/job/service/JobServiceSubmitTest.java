package com.sudhanva.task_forge.job.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

// Jackson 3 packages. If an import is red, let the IDE fix it and note it in docs/syntax-notes.md.
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

// Plain JUnit + Mockito. No Spring, no database.
// REAL: store, thread pool, JSON mapper, validator.
// FAKE: the job handler (FakeHandler below), so each test controls what a job does.
//
// How we wait without sleeping:
//   - "gate"  : a latch that blocks the handler until the test opens it.
//   - pool.shutdown() + awaitTermination: waits until every queued job has really finished.
class JobServiceSubmitTest {

    private static final int WAIT_SECONDS = 5;

    private final JsonMapper mapper = JsonMapper.builder().build();
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private InMemoryJobStore store;     // a Mockito spy: real behavior, but we can verify calls
    private FakeHandler handler;
    private ThreadPoolExecutor pool;
    private JobService service;

    @BeforeEach
    void setUp() {
        store = spy(new InMemoryJobStore());
        handler = new FakeHandler();
        pool = newPool(2, 2, 10);
        service = newService(pool, new JobHandlerRegistry(List.of(handler)));
    }

    @AfterEach
    void tearDown() {
        handler.gate.countDown();   // never leave a worker stuck
        pool.shutdownNow();
    }

    // ---------------- helpers ----------------

    private ThreadPoolExecutor newPool(int core, int max, int queueSize) {
        return new ThreadPoolExecutor(
            core, max, 0L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(queueSize),
            new ThreadPoolExecutor.AbortPolicy()
        );
    }

    // Order must match the field order in JobService (@RequiredArgsConstructor).
    private JobService newService(ThreadPoolExecutor p, JobHandlerRegistry registry) {
        return new JobService(store, p, registry, mapper, validator);
    }

    // swap to a smaller pool for tests that need to fill it up
    private void useSmallPool(int core, int max, int queueSize) {
        pool.shutdownNow();
        pool = newPool(core, max, queueSize);
        service = newService(pool, new JobHandlerRegistry(List.of(handler)));
    }

    private SubmitJobRequest echo(String message, long delayMs) {
        return requestWith("{\"message\":\"" + message + "\",\"delayMs\":" + delayMs + "}");
    }

    private SubmitJobRequest requestWith(String payloadJson) {
        JsonNode payload = mapper.readTree(payloadJson);
        return new SubmitJobRequest(JobType.ECHO, payload);
    }

    // open the gate, then wait until every accepted job has finished
    private void finishAllJobs() throws Exception {
        handler.gate.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(WAIT_SECONDS, TimeUnit.SECONDS), "jobs did not finish in time");
    }

    // ==========================================================
    // submit(): the happy path
    // ==========================================================

    @Test
    void submit_validRequest_returnsQueuedJobWithIdAndType() {
        Job job = service.submit(echo("hi", 0));

        assertNotNull(job.id());
        assertEquals(JobStatus.QUEUED, job.status());   // the returned record is a snapshot at submit time
        assertEquals(JobType.ECHO, job.type());
    }

    @Test
    void submit_validRequest_jobIsInTheStore() {
        Job job = service.submit(echo("hi", 0));

        assertDoesNotThrow(() -> service.getJob(job.id()));
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
    // submit(): bad input. Must fail BEFORE any state change.
    // ==========================================================

    @Test
    void submit_payloadWithWrongShape_throwsInvalidPayload() {
        assertThrows(InvalidPayloadException.class,
            () -> service.submit(requestWith("{\"x\":1}")));
    }

    @Test
    void submit_blankMessage_throwsInvalidPayload() {
        assertThrows(InvalidPayloadException.class,
            () -> service.submit(requestWith("{\"message\":\"  \",\"delayMs\":0}")));
    }

    @Test
    void submit_delayTooBig_throwsInvalidPayload() {
        assertThrows(InvalidPayloadException.class,
            () -> service.submit(requestWith("{\"message\":\"hi\",\"delayMs\":9001}")));
    }

    @Test
    void submit_negativeDelay_throwsInvalidPayload() {
        assertThrows(InvalidPayloadException.class,
            () -> service.submit(requestWith("{\"message\":\"hi\",\"delayMs\":-1}")));
    }

    @Test
    void submit_nullPayload_throwsInvalidPayload() {
        assertThrows(InvalidPayloadException.class,
            () -> service.submit(requestWith("null")));
    }

    @Test
    void submit_invalidPayload_savesNothingAndRunsNothing() throws Exception {
        assertThrows(InvalidPayloadException.class,
            () -> service.submit(requestWith("{\"x\":1}")));

        verify(store, never()).save(any());
        finishAllJobs();
        assertEquals(0, handler.calls.get());
    }

    @Test
    void submit_typeWithNoRegisteredHandler_throwsIllegalArgument() {
        // Current behavior: the registry throws IllegalArgumentException.
        // This is your OPEN item 4. If you map it to a 400, change this test on purpose.
        JobService noHandlers = newService(pool, new JobHandlerRegistry(List.of()));

        assertThrows(IllegalArgumentException.class, () -> noHandlers.submit(echo("hi", 0)));
        verify(store, never()).save(any());
    }

    // ==========================================================
    // submit(): the pool is full or shut down
    // ==========================================================

    @Test
    void submit_poolAndQueueFull_throwsQueueFull() throws Exception {
        useSmallPool(1, 1, 1);
        handler.gate = new CountDownLatch(1);               // jobs block until we open the gate

        service.submit(echo("running", 0));                 // takes the only worker
        assertTrue(handler.firstStarted.await(WAIT_SECONDS, TimeUnit.SECONDS));
        service.submit(echo("queued", 0));                  // takes the only queue slot

        assertThrows(QueueFullException.class, () -> service.submit(echo("rejected", 0)));
    }

    @Test
    void submit_queueFull_removesTheRejectedJobFromTheStore() throws Exception {
        useSmallPool(1, 1, 1);
        handler.gate = new CountDownLatch(1);

        service.submit(echo("running", 0));
        assertTrue(handler.firstStarted.await(WAIT_SECONDS, TimeUnit.SECONDS));
        service.submit(echo("queued", 0));
        assertThrows(QueueFullException.class, () -> service.submit(echo("rejected", 0)));

        // 3 saves happened. The 3rd one was rejected, so it must be gone. The first two stay.
        ArgumentCaptor<Job> saved = ArgumentCaptor.forClass(Job.class);
        verify(store, times(3)).save(saved.capture());
        List<Job> jobs = saved.getAllValues();
        assertTrue(store.findById(jobs.get(0).id()).isPresent());
        assertTrue(store.findById(jobs.get(1).id()).isPresent());
        assertTrue(store.findById(jobs.get(2).id()).isEmpty());
    }

    @Test
    void submit_executorShutDown_throwsExecutorShutdownNotQueueFull() {
        pool.shutdown();

        assertThrows(ExecutorShutdownException.class, () -> service.submit(echo("hi", 0)));
    }

    @Test
    void submit_executorShutDown_removesTheJobFromTheStore() {
        pool.shutdown();

        assertThrows(ExecutorShutdownException.class, () -> service.submit(echo("hi", 0)));

        ArgumentCaptor<Job> saved = ArgumentCaptor.forClass(Job.class);
        verify(store).save(saved.capture());
        assertTrue(store.findById(saved.getValue().id()).isEmpty());
    }

    // ==========================================================
    // runJob(): what the worker thread does with the status
    // ==========================================================

    @Test
    void runJob_whileHandlerIsRunning_statusIsRunning() throws Exception {
        handler.gate = new CountDownLatch(1);

        Job job = service.submit(echo("hi", 0));
        assertTrue(handler.firstStarted.await(WAIT_SECONDS, TimeUnit.SECONDS));

        assertEquals(JobStatus.RUNNING, service.getJob(job.id()).status());
        finishAllJobs();
    }

    @Test
    void runJob_handlerSucceeds_statusBecomesSucceeded() throws Exception {
        Job job = service.submit(echo("hi", 0));
        finishAllJobs();

        assertEquals(JobStatus.SUCCEEDED, service.getJob(job.id()).status());
    }

    @Test
    void runJob_handlerThrows_statusBecomesFailed() throws Exception {
        Job job = service.submit(echo(FakeHandler.FAIL_MESSAGE, 0));
        finishAllJobs();

        assertEquals(JobStatus.FAILED, service.getJob(job.id()).status());
    }

    @Test
    void runJob_afterAFailure_theWorkerStillRunsTheNextJob() throws Exception {
        useSmallPool(1, 1, 5);      // ONE worker thread, so the same thread must survive the failure

        Job bad = service.submit(echo(FakeHandler.FAIL_MESSAGE, 0));
        Job good = service.submit(echo("fine", 0));
        finishAllJobs();

        assertEquals(JobStatus.FAILED, service.getJob(bad.id()).status());
        assertEquals(JobStatus.SUCCEEDED, service.getJob(good.id()).status());
    }

    @Test
    void runJob_jobRemovedBeforeItStarts_handlerNeverRunsAndNoGhostAppears() throws Exception {
        useSmallPool(1, 1, 5);
        handler.gate = new CountDownLatch(1);

        service.submit(echo("blocker", 0));                       // holds the only worker
        assertTrue(handler.firstStarted.await(WAIT_SECONDS, TimeUnit.SECONDS));
        Job waiting = service.submit(echo("waiting", 0));         // sits in the queue
        store.remove(waiting.id());                               // removed before a worker picks it up

        finishAllJobs();

        assertEquals(1, handler.calls.get());                     // only the blocker ran
        assertTrue(store.findById(waiting.id()).isEmpty());       // and nothing revived the removed job
    }

    @Test
    void runJob_manyJobs_allEndSucceeded() throws Exception {
        useSmallPool(4, 4, 50);
        List<Job> jobs = new ArrayList<>();

        for (int i = 0; i < 40; i++) {
            jobs.add(service.submit(echo("job-" + i, 0)));
        }
        finishAllJobs();

        assertEquals(40, handler.calls.get());
        for (Job job : jobs) {
            assertEquals(JobStatus.SUCCEEDED, service.getJob(job.id()).status());
        }
    }

    // ==========================================================
    // A fake handler we fully control
    // ==========================================================

    static class FakeHandler implements JobHandler<EchoPayload, String> {

        static final String FAIL_MESSAGE = "bad";   // a payload with this message makes the handler throw

        final AtomicInteger calls = new AtomicInteger();
        final List<EchoPayload> received = new CopyOnWriteArrayList<>();
        final CountDownLatch firstStarted = new CountDownLatch(1);
        // open (count 0) by default. A test replaces it with new CountDownLatch(1) to block jobs.
        volatile CountDownLatch gate = new CountDownLatch(0);

        @Override
        public String execute(EchoPayload payload) throws Exception {
            calls.incrementAndGet();
            received.add(payload);
            firstStarted.countDown();
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