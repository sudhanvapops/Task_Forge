package com.sudhanva.task_forge.job.model;

import java.util.UUID;

import com.sudhanva.task_forge.job.interfaces.JobType;
import com.sudhanva.task_forge.job.model.enums.JobStatus;

public record Job(
    UUID id,
    JobStatus status,
    JobType type
) {}
