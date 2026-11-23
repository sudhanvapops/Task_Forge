package com.sudhanva.task_forge.config;

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.sudhanva.task_forge.executor.JobExecutorFactory;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableAsync
// @RequiredArgsConstructor
public class ExecutorConfig {
    
    // private final JobExecutorFactory executorFactory;

    // Qualifier if you have two beans and how to shutdown
    // @Bean(name = "jobExecutor", destroyMethod = "shutdown")
    // public ThreadPoolExecutor jobExecutor() {
    //     return executorFactory.createExecutor();
    // }


    // now using the sorings own ThreadPoolTaskExecutor bean for this
    @Bean
    public ThreadPoolTaskExecutor jobExecutor(){

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(10);
        executor.setKeepAliveSeconds(30);
        executor.setThreadNamePrefix("job-worker-");
        executor.setRejectedExecutionHandler(
                new ThreadPoolExecutor.AbortPolicy()
        );

        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }


}
