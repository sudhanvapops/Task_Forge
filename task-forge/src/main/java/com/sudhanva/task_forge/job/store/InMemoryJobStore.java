package com.sudhanva.task_forge.job.store;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.sudhanva.task_forge.job.exceptions.DuplicateJobException;
import com.sudhanva.task_forge.job.model.Job;
import com.sudhanva.task_forge.job.model.enums.JobStatus;

@Component
public class InMemoryJobStore implements JobStore{

    private final Map<UUID,Job> jobs = new ConcurrentHashMap<>();

    @Override
    public Optional<Job> findById(UUID id) {
        Job job = jobs.get(id);
        return Optional.ofNullable(job);
    }

    @Override
    public boolean remove(UUID id) {
        Job job = jobs.remove(id);
        return job != null;
    }

    @Override
    public void save(Job job) {
        // Service in the request stores the job so exception is handeled in global controller advice
        Job existing = jobs.putIfAbsent(job.id(), job);
         if (existing != null) {
            // thrown 409 CONLICT 
            throw new DuplicateJobException(job.id());
        }
    }

    @Override
    public boolean updateStatus(UUID id, JobStatus newStatus) {
        // update is done by worker thread so .orElseThrow() 
        // there is no http request waiting for this it wont reach controller advice
        Job job = this.findById(id).orElseThrow();
        Job newJob = new Job(
            id,
            newStatus,
            job.type()
        );
        jobs.replace(id, newJob);
        return true;
    }
    
}
