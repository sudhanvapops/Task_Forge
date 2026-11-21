package com.sudhanva.task_forge.config;

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WorkerPoolStartupCheck implements ApplicationRunner {

    private final ThreadPoolExecutor jobExecutor;

    @Override
    public void run(ApplicationArguments args) {

        System.out.println("========== WORKER POOL ==========");
        System.out.println("Core Pool Size: "
                + jobExecutor.getCorePoolSize());

        System.out.println("Maximum Pool Size: "
                + jobExecutor.getMaximumPoolSize());

        System.out.println("Queue Capacity: "
                + (jobExecutor.getQueue().size()
                        + jobExecutor.getQueue().remainingCapacity()));

        System.out.println("=================================");
    }

}
