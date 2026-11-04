package com.sudhanva.task_forge.job;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

// dleagets teh work to appropriate handler
@Component
public class JobHandlerRegistry {
    
    private final Map<JobType,JobHandler<?,?>> jobHandlers;

    public JobHandlerRegistry(List<JobHandler<?,?>> handlers){
        this.jobHandlers =  handlers.stream().collect(
            Collectors.toUnmodifiableMap( 
                handler -> handler.type(), 
                handler -> handler
            )
        );
    }

    public JobHandler<?,?> getHandler(JobType type){
        JobHandler<?,?> handler = jobHandlers.get(type);
        if (handler == null){
           throw new IllegalArgumentException(
                "No handler for: " + type +
                ". Registered handlers: " + jobHandlers.keySet()
            );
        }
            
        return handler;
    }

    // The compiler still cannot prove that the handler and payload match
    // The runtime class check provides that guarantee

    

    
    
}
