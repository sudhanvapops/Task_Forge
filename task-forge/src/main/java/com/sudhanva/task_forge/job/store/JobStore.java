package com.sudhanva.task_forge.job.store;

// import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sudhanva.task_forge.job.model.Job;
import com.sudhanva.task_forge.job.model.enums.JobStatus;

public interface JobStore {
    // save (create)
    void save(Job job);
    // findAll
    // List<JobStatus> findAllJobs();
    // findById
    Optional<Job> findById(UUID id);
    // update status
    boolean updateStatus(UUID id,JobStatus newStatus);
    // remove
    boolean remove(UUID id);
}
