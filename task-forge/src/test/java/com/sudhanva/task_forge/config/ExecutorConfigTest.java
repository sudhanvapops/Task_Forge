package com.sudhanva.task_forge.config;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

// Plain JUnit. Builds the executor straight from the config method.
class ExecutorConfigTest {

    private ThreadPoolTaskExecutor executor;
    private final CountDownLatch gate = new CountDownLatch(1);

    @BeforeEach
    void setUp() {
        executor = new ExecutorConfig().jobExecutor();
        executor.initialize();     // Spring does this for a real bean
    }

    @AfterEach
    void tearDown() {
        gate.countDown();          // never leave workers stuck
        executor.getThreadPoolExecutor().shutdownNow();
    }

    @Test
    void settings_matchTheA2Pool() {
        assertEquals(2, executor.getCorePoolSize());
        assertEquals(4, executor.getMaxPoolSize());
        assertEquals(10, executor.getQueueCapacity());
    }

    @Test
    void workerThreads_useTheJobWorkerPrefix() throws Exception {
        AtomicReference<String> name = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);

        executor.execute(() -> {
            name.set(Thread.currentThread().getName());
            done.countDown();
        });

        assertTrue(done.await(5, TimeUnit.SECONDS));
        assertTrue(name.get().startsWith("job-worker-"));
    }

    @Test
    void sixteenBlockedJobs_fourteenAccepted_twoRejected() {
        int accepted = 0;
        int rejected = 0;

        for (int i = 0; i < 16; i++) {
            try {
                executor.execute(() -> {
                    try { gate.await(); } catch (InterruptedException ignored) { }
                });
                accepted++;
            } catch (RejectedExecutionException e) {
                rejected++;
            }
        }

        assertEquals(14, accepted);   // 2 core + 10 queue + 2 extra workers
        assertEquals(2, rejected);
    }

    @Test
    void shutdown_waitsForRunningAndQueuedJobs() throws Exception {
        AtomicInteger finished = new AtomicInteger();
        CountDownLatch firstStarted = new CountDownLatch(1);

        // 3 jobs: 2 run on core threads, 1 waits in the queue
        for (int i = 0; i < 3; i++) {
            executor.execute(() -> {
                firstStarted.countDown();
                try {
                    Thread.sleep(300);
                    finished.incrementAndGet();
                } catch (InterruptedException ignored) { }
            });
        }
        assertTrue(firstStarted.await(5, TimeUnit.SECONDS));

        executor.shutdown();          // should block until all 3 are done

        assertEquals(3, finished.get());
    }
}