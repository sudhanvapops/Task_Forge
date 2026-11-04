package com.sudhanva.task_forge.A_Tests;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.sudhanva.task_forge.job.JobHandler;
import com.sudhanva.task_forge.job.JobHandlerRegistry;
import com.sudhanva.task_forge.job.JobType;
import com.sudhanva.task_forge.job.handler.EchoJobHandler;

@SpringBootTest
@Testcontainers 
public class JobHandlerRegistryTest {
    
    @Autowired 
    JobHandlerRegistry registry;

    @Container 
    @ServiceConnection
    static PostgreSQLContainer<?> pg = 
        new PostgreSQLContainer<>("postgres:16-alpine");


    @Test
    void shouldRegisterEchoHandler(){
        JobHandler<?,?> handler = registry.getHandler(JobType.ECHO);
        System.out.println(handler);
        assertNotNull(handler);
        assertInstanceOf(EchoJobHandler.class, handler);
    }

}
