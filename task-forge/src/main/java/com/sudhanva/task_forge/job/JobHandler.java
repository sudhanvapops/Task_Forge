package com.sudhanva.task_forge.job;

// Interface for each handlers
// P specifc payload class
// R result retun by the handler
public interface JobHandler <P extends JobPayload, R>{
    
    // gives what type of payload is
    JobType type(); 
    // process the types payload and return a result
    R execute(P payload) throws Exception;
    // To retorve the class 
    // beacuse while casting The regestry losses the class Information
    // P tells the compiler the type; Class<P> lets us check and cast objects to that type at runtime.
    Class<P> payloadType();

    default R executeUntyped(JobPayload payload) throws Exception {
        return execute(payloadType().cast(payload));
    }
}
