package com.sudhanva.task_forge.job.api.controller;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.sudhanva.task_forge.common.api.ApiResponse;
import com.sudhanva.task_forge.job.api.dto.JobResponse;
import com.sudhanva.task_forge.job.api.dto.SubmitJobRequest;
import com.sudhanva.task_forge.job.model.Job;
import com.sudhanva.task_forge.job.service.JobService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;


    // Utility Methods

    private JobResponse mapToJobResponse(
        Job job
    ){
        JobResponse data = new JobResponse(job.id(), job.status(),job.type());
        return data;
    }
    

    // Mappings
    @PostMapping("/post-job")
    public ResponseEntity<ApiResponse<JobResponse>> acceptJob(
        @Valid @RequestBody SubmitJobRequest request
    ){

        Job job = jobService.submit(request);

        // Location URI
        URI location = ServletUriComponentsBuilder
                            .fromCurrentContextPath()
                            .path("/api/jobs/{id}")
                            .buildAndExpand(job.id())
                            .toUri();
        
        // map to response
        ApiResponse<JobResponse> data = new ApiResponse<>(
            true,
            "Job Accepted: "+job.id(),
            mapToJobResponse(job)
        );

        return ResponseEntity
                    .accepted()
                    .location(location)
                    .body(data);
    }


    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JobResponse>> get(@PathVariable UUID id) {
        Job job = jobService.getJob(id);

        // map to response
        ApiResponse<JobResponse> data = new ApiResponse<>(
            true,
            "Job Status",
            mapToJobResponse(job)
        );
        return ResponseEntity.ok(data);
    }

}
