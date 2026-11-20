package com.sudhanva.task_forge.job.api.dto;

import tools.jackson.databind.JsonNode;
import com.sudhanva.task_forge.job.interfaces.JobType;

import jakarta.validation.constraints.NotNull;

public record SubmitJobRequest(
    @NotNull(message = "Job type is required") JobType type,
    @NotNull(message = "Payload is required") JsonNode payload

) {
    
}
