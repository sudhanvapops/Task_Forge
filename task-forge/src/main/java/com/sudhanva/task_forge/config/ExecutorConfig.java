package com.sudhanva.task_forge.config;

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sudhanva.task_forge.executor.JobExecutorFactory;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class ExecutorConfig {
    
    private final JobExecutorFactory executorFactory;

    // Qualifier if you have two beans and how to shutdown
    @Bean(name = "jobExecutor", destroyMethod = "shutdown")
    public ThreadPoolExecutor jobExecutor() {
        return executorFactory.createExecutor();
    }

}
