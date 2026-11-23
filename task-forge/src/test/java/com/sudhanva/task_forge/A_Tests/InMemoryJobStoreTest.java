package com.sudhanva.task_forge.A_Tests;


import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sudhanva.task_forge.job.exceptions.DuplicateJobException;
import com.sudhanva.task_forge.job.interfaces.JobType;
import com.sudhanva.task_forge.job.model.Job;
import com.sudhanva.task_forge.job.model.enums.JobStatus;
import com.sudhanva.task_forge.job.store.InMemoryJobStore;

// Plain JUnit. No Spring, no database.
// A fresh store is built before every test, so tests never affect each other.
class InMemoryJobStoreTest {

    private InMemoryJobStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryJobStore();
    }

    // small helper so each test stays short
    private Job newJob() {
        return new Job(UUID.randomUUID(), JobStatus.QUEUED, JobType.ECHO);
    }

    // ==========================================================
    // save + findById
    // ==========================================================

    @Test
    void save_thenFindById_returnsSameJob() {
        Job job = newJob();

        store.save(job);

        assertEquals(job, store.findById(job.id()).orElseThrow());
    }

    @Test
    void findById_unknownId_returnsEmpty() {
        assertTrue(store.findById(UUID.randomUUID()).isEmpty());
    }

    @Test
    void save_twoDifferentJobs_bothAreStored() {
        Job a = newJob();
        Job b = newJob();

        store.save(a);
        store.save(b);

        assertEquals(a, store.findById(a.id()).orElseThrow());
        assertEquals(b, store.findById(b.id()).orElseThrow());
    }

    @Test
    void save_sameIdTwice_throwsDuplicateJobException() {
        Job job = newJob();
        store.save(job);

        assertThrows(DuplicateJobException.class, () -> store.save(job));
    }

    @Test
    void save_sameIdTwice_keepsTheOriginalJob() {
        Job original = newJob();
        Job sameIdOtherStatus = new Job(original.id(), JobStatus.FAILED, JobType.ECHO);
        store.save(original);

        assertThrows(DuplicateJobException.class, () -> store.save(sameIdOtherStatus));

        // the failed save must not overwrite anything
        assertEquals(JobStatus.QUEUED, store.findById(original.id()).orElseThrow().status());
    }

    // ==========================================================
    // updateStatus
    // ==========================================================

    @Test
    void updateStatus_existingJob_returnsTrueAndChangesStatus() {
        Job job = newJob();
        store.save(job);

        boolean updated = store.updateStatus(job.id(), JobStatus.RUNNING);

        assertTrue(updated);
        assertEquals(JobStatus.RUNNING, store.findById(job.id()).orElseThrow().status());
    }

    @Test
    void updateStatus_keepsIdAndTypeUnchanged() {
        Job job = newJob();
        store.save(job);

        store.updateStatus(job.id(), JobStatus.SUCCEEDED);

        Job after = store.findById(job.id()).orElseThrow();
        assertEquals(job.id(), after.id());
        assertEquals(job.type(), after.type());
    }

    @Test
    void updateStatus_canMoveThroughSeveralStatuses() {
        Job job = newJob();
        store.save(job);

        store.updateStatus(job.id(), JobStatus.RUNNING);
        store.updateStatus(job.id(), JobStatus.SUCCEEDED);

        assertEquals(JobStatus.SUCCEEDED, store.findById(job.id()).orElseThrow().status());
    }

    @Test
    void updateStatus_unknownId_returnsFalse() {
        assertFalse(store.updateStatus(UUID.randomUUID(), JobStatus.RUNNING));
    }

    @Test
    void updateStatus_unknownId_doesNotCreateAGhostEntry() {
        UUID unknown = UUID.randomUUID();

        store.updateStatus(unknown, JobStatus.RUNNING);

        assertTrue(store.findById(unknown).isEmpty());
    }

    @Test
    void updateStatus_onlyChangesTheTargetJob() {
        Job a = newJob();
        Job b = newJob();
        store.save(a);
        store.save(b);

        store.updateStatus(a.id(), JobStatus.FAILED);

        assertEquals(JobStatus.FAILED, store.findById(a.id()).orElseThrow().status());
        assertEquals(JobStatus.QUEUED, store.findById(b.id()).orElseThrow().status());
    }

    // ==========================================================
    // remove
    // ==========================================================

    @Test
    void remove_existingJob_returnsTrueAndJobIsGone() {
        Job job = newJob();
        store.save(job);

        assertTrue(store.remove(job.id()));
        assertTrue(store.findById(job.id()).isEmpty());
    }

    @Test
    void remove_unknownId_returnsFalse() {
        assertFalse(store.remove(UUID.randomUUID()));
    }

    @Test
    void remove_twice_secondCallReturnsFalse() {
        Job job = newJob();
        store.save(job);

        assertTrue(store.remove(job.id()));
        assertFalse(store.remove(job.id()));
    }

    @Test
    void remove_onlyRemovesTheTargetJob() {
        Job a = newJob();
        Job b = newJob();
        store.save(a);
        store.save(b);

        store.remove(a.id());

        assertTrue(store.findById(b.id()).isPresent());
    }

    @Test
    void updateStatus_afterRemove_returnsFalseAndDoesNotBringJobBack() {
        // this is the real submit() case: queue full -> remove, a worker must not revive it
        Job job = newJob();
        store.save(job);
        store.remove(job.id());

        assertFalse(store.updateStatus(job.id(), JobStatus.RUNNING));
        assertTrue(store.findById(job.id()).isEmpty());
    }

    @Test
    void save_afterRemove_sameIdIsAllowedAgain() {
        Job job = newJob();
        store.save(job);
        store.remove(job.id());

        assertDoesNotThrow(() -> store.save(job));
    }

    // ==========================================================
    // many threads
    // Idea: start all threads at the same instant (latch), so they really overlap.
    // ==========================================================

    private static final int THREADS = 16;

    @Test
    void manyThreads_savingDifferentJobs_allAreStored() throws Exception {
        int jobsPerThread = 200;
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch startGate = new CountDownLatch(1);
        List<Job> allJobs = new ArrayList<>();
        List<Future<?>> futures = new ArrayList<>();

        for (int t = 0; t < THREADS; t++) {
            List<Job> mine = new ArrayList<>();
            for (int i = 0; i < jobsPerThread; i++) {
                mine.add(newJob());
            }
            allJobs.addAll(mine);
            futures.add(pool.submit(() -> {
                startGate.await();
                mine.forEach(store::save);
                return null;
            }));
        }

        startGate.countDown();
        waitForAll(futures);
        pool.shutdown();

        for (Job job : allJobs) {
            assertTrue(store.findById(job.id()).isPresent(), "lost job " + job.id());
        }
    }

    @Test
    void manyThreads_savingTheSameId_exactlyOneWins() throws Exception {
        Job job = newJob();
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger duplicates = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        for (int t = 0; t < THREADS; t++) {
            futures.add(pool.submit(() -> {
                startGate.await();
                try {
                    store.save(job);
                    successes.incrementAndGet();
                } catch (DuplicateJobException e) {
                    duplicates.incrementAndGet();
                }
                return null;
            }));
        }

        startGate.countDown();
        waitForAll(futures);
        pool.shutdown();

        assertEquals(1, successes.get());
        assertEquals(THREADS - 1, duplicates.get());
    }

    @Test
    void manyThreads_updatingTheSameJob_noUpdateIsLostOrBroken() throws Exception {
        // each thread writes its own status many times.
        // At the end the job must still exist, with a valid status, and the same id and type.
        Job job = newJob();
        store.save(job);

        JobStatus[] statuses = {JobStatus.RUNNING, JobStatus.SUCCEEDED, JobStatus.FAILED};
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger falseResults = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        for (int t = 0; t < THREADS; t++) {
            JobStatus mine = statuses[t % statuses.length];
            futures.add(pool.submit(() -> {
                startGate.await();
                for (int i = 0; i < 500; i++) {
                    if (!store.updateStatus(job.id(), mine)) {
                        falseResults.incrementAndGet();
                    }
                }
                return null;
            }));
        }

        startGate.countDown();
        waitForAll(futures);
        pool.shutdown();

        assertEquals(0, falseResults.get(), "job exists, so every update must return true");
        Job after = store.findById(job.id()).orElseThrow();
        assertEquals(job.id(), after.id());
        assertEquals(job.type(), after.type());
        assertTrue(List.of(statuses).contains(after.status()));
    }

    @Test
    void manyThreads_updateRacingWithRemove_neverLeavesAGhost() throws Exception {
        // One thread removes the job. Other threads keep updating it.
        // After the remove, the job must be gone for good. An update must not bring it back.
        int rounds = 300;

        for (int r = 0; r < rounds; r++) {
            InMemoryJobStore freshStore = new InMemoryJobStore();
            Job job = newJob();
            freshStore.save(job);

            ExecutorService pool = Executors.newFixedThreadPool(4);
            CountDownLatch startGate = new CountDownLatch(1);
            List<Future<?>> futures = new ArrayList<>();

            futures.add(pool.submit(() -> {
                startGate.await();
                freshStore.remove(job.id());
                return null;
            }));
            for (int t = 0; t < 3; t++) {
                futures.add(pool.submit(() -> {
                    startGate.await();
                    for (int i = 0; i < 50; i++) {
                        freshStore.updateStatus(job.id(), JobStatus.RUNNING);
                    }
                    return null;
                }));
            }

            startGate.countDown();
            waitForAll(futures);
            pool.shutdown();

            assertTrue(freshStore.findById(job.id()).isEmpty(), "ghost entry in round " + r);
        }
    }

    // waits for every task and rethrows the first error, so a failure inside a thread fails the test
    private void waitForAll(List<Future<?>> futures) throws Exception {
        for (Future<?> f : futures) {
            f.get(10, TimeUnit.SECONDS);
        }
    }
}