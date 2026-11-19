package com.sudhanva.task_forge.A_Tests;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.sudhanva.task_forge.job.handler.EchoJobHandler;
import com.sudhanva.task_forge.job.handler.EchoPayload;
import com.sudhanva.task_forge.job.interfaces.JobHandler;
import com.sudhanva.task_forge.job.interfaces.JobHandlerRegistry;
import com.sudhanva.task_forge.job.interfaces.JobType;

public class JobHandlerTests {
    
    @Test
    void unkownJobType(){
        
        JobHandlerRegistry registry = 
            new JobHandlerRegistry(List.of());


        assertThrows(
            IllegalArgumentException.class, 
            () -> {
                registry.getHandler(JobType.ECHO);
            }
        );
    }

    @Test
    void duplicateJobType(){
        JobHandler<EchoPayload,String> handler = 
                new EchoJobHandler();

        JobHandler<EchoPayload,String> handler2 = 
                new EchoJobHandler();
        
        assertThrows(
            IllegalStateException.class, 
            () -> {
                new JobHandlerRegistry(List.of(handler,handler2));
            }
        );
    }

}
