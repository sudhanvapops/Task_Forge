package com.sudhanva.task_forge.scratch;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.sudhanva.task_forge.executor.JobExecutorFactory;

public class A2PoolDemo {
 
    public static void main(String[] args) {
        
        // JobExecutorFactory factory = new JobExecutorFactory();
        
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        for (int i = 1; i <= 16; i++) {

            int taskNo = i;

            try {
                executor.execute(() -> {
                    // Print task number and thread name
                    System.out.println("Task No: "+taskNo + " Thread Name: "+Thread.currentThread().getName());
                    try {
                        // Sleep for 30 seconds
                        // another thread can interrupt a sleeping thread before those 30 seconds are over.
                        Thread.sleep(9000);    
                    } catch (InterruptedException e) {
                        System.out.println("Worker interrupted"+Thread.currentThread().getName());
                        // pool.shutdown();
                    }
                });
                System.out.println("Job Accepted: "+taskNo);
            } catch (RejectedExecutionException e) {
                System.out.println("Job Rejected: "+taskNo);
            }
        }

        // Let running tasks finish, accept no new ones
        executor.shutdown();
    }
    
}
