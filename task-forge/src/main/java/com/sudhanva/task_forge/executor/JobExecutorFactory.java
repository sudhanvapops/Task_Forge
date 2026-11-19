package com.sudhanva.task_forge.executor;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

// Create a configured ThreadPoolExecutor 
// and return it to whoever requests it
// Custom ThreadPoolFactory for my Wants
public class JobExecutorFactory {

    // Thread Factory
    private final AtomicInteger counter = new AtomicInteger(1);

    private final ThreadFactory factory = 
            runnable -> new Thread(
                runnable,
                "job-worker-"+counter.getAndIncrement()
            );


    // Create and return the pool
    public ThreadPoolExecutor createExecutor(){
        ThreadPoolExecutor pool = new ThreadPoolExecutor(
            2, 
            4,
            // This is for new worker cretaed and how much time keep them alive if idle
            30, 
            TimeUnit.SECONDS,
            // Waits in push and pop operation when ful lor empty
            new ArrayBlockingQueue<>(10),
            // new LinkedBlockingQueue<>(),
            factory,
            new ThreadPoolExecutor.AbortPolicy()
        );

        return pool;
    }
   
}
