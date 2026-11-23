package com.sudhanva.task_forge.config;

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WorkerPoolStartupCheck implements ApplicationRunner {

    private final ThreadPoolTaskExecutor jobExecutor;

    @Override
    public void run(ApplicationArguments args) {

        System.out.println("========== WORKER POOL ==========");
        System.out.println("Core Pool Size: "
                + jobExecutor.getCorePoolSize());

        System.out.println("Maximum Pool Size: "
                + jobExecutor.getMaxPoolSize());

        System.out.println("Queue Capacity: "
                + (jobExecutor.getQueueCapacity()));

        System.out.println("=================================");
    }

}
