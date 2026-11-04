package com.sudhanva.task_forge.job.handler;

import org.springframework.stereotype.Component;

import com.sudhanva.task_forge.job.JobHandler;
import com.sudhanva.task_forge.job.JobType;

@Component 
public class EchoJobHandler implements JobHandler<EchoPayload, String>{

    @Override
    public String execute(EchoPayload payload) throws Exception {
        Thread.sleep(payload.delayMs()); 
        return "Echoed: "+payload.message();
    }

    @Override
    public JobType type() {
        return JobType.ECHO;
    }

    @Override
    public Class<EchoPayload> payloadType() {
        return EchoPayload.class;
    }

    
    
}
