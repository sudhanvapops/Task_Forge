package com.sudhanva.task_forge.job.api.dto;

import java.util.UUID;

import com.sudhanva.task_forge.job.interfaces.JobType;
import com.sudhanva.task_forge.job.model.enums.JobStatus;

import lombok.Builder;

@Builder
public record JobResponse(
    UUID id,
    JobStatus status,
    JobType type
) {
    
}
