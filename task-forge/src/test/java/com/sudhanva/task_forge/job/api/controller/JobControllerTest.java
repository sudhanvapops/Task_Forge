package com.sudhanva.task_forge.job.api.controller;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.sudhanva.task_forge.job.exceptions.DuplicateJobException;
import com.sudhanva.task_forge.job.exceptions.ExecutorShutdownException;
import com.sudhanva.task_forge.job.exceptions.InvalidPayloadException;
import com.sudhanva.task_forge.job.exceptions.JobNotFoundException;
import com.sudhanva.task_forge.job.exceptions.QueueFullException;
import com.sudhanva.task_forge.job.interfaces.JobType;
import com.sudhanva.task_forge.job.model.Job;
import com.sudhanva.task_forge.job.model.enums.JobStatus;
import com.sudhanva.task_forge.job.service.JobService;

// Web layer only: controller + controller advice + JSON. JobService is a FAKE (mock).
// So these tests check HTTP behavior only: status codes, headers, JSON shape.
@WebMvcTest(JobController.class)
class JobControllerTest {

   
    private static final String ID_PATH = "$.data.id";
    private static final String STATUS_PATH = "$.data.status";
    private static final String TYPE_PATH = "$.data.type";
   
    private static final int QUEUE_FULL_HTTP_STATUS = 503;

    private static final String VALID_BODY = """
        {"type":"ECHO","payload":{"message":"hi","delayMs":10}}
        """;

    @Autowired
    MockMvc mvc;

    @MockitoBean
    JobService jobService;

    private Job job(JobStatus status) {
        return new Job(UUID.randomUUID(), status, JobType.ECHO);
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder postJob(String body) {
        return post("/api/jobs/post-job").contentType(MediaType.APPLICATION_JSON).content(body);
    }

    // ==========================================================
    // POST /jobs : success
    // ==========================================================

    // @Test
    void post_validRequest_returns202() throws Exception {
        when(jobService.submit(any())).thenReturn(job(JobStatus.QUEUED));

        mvc.perform(postJob(VALID_BODY))
            .andExpect(status().isAccepted());
    }

    // @Test
    void post_validRequest_returnsLocationHeaderPointingToTheNewJob() throws Exception {
        Job saved = job(JobStatus.QUEUED);
        when(jobService.submit(any())).thenReturn(saved);

        mvc.perform(postJob(VALID_BODY))
            .andExpect(header().string("Location", endsWith("/api/jobs/" + saved.id())));
    }

    // @Test
    void post_validRequest_bodyHasIdStatusAndType() throws Exception {
        Job saved = job(JobStatus.QUEUED);
        when(jobService.submit(any())).thenReturn(saved);

        mvc.perform(postJob(VALID_BODY))
            .andExpect(jsonPath(ID_PATH).value(saved.id().toString()))
            .andExpect(jsonPath(STATUS_PATH).value("QUEUED"))
            .andExpect(jsonPath(TYPE_PATH).value("ECHO"));
    }

    // ==========================================================
    // POST /jobs : bad input must be 400, and the service must NOT be called
    // ==========================================================

    // @Test
    void post_missingPayload_returns400() throws Exception {
        mvc.perform(postJob("""
                {"type":"ECHO"}
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(jobService);   // fails if @Valid is missing
    }

    // @Test
    void post_missingType_returns400() throws Exception {
        mvc.perform(postJob("""
                {"payload":{"message":"hi","delayMs":10}}
                """))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(jobService);
    }

    // @Test
    void post_unknownJobType_returns400() throws Exception {
        mvc.perform(postJob("""
                {"type":"NOPE","payload":{}}
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"));

        verifyNoInteractions(jobService);
    }

    // @Test
    void post_brokenJson_returns400() throws Exception {
        mvc.perform(postJob("{ this is not json"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"));
    }

    // @Test
    void post_emptyBody_returns400() throws Exception {
        mvc.perform(post("/api/jobs/post-job").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    // @Test
    void post_serviceRejectsPayload_returns400WithInvalidPayloadCode() throws Exception {
        when(jobService.submit(any())).thenThrow(new InvalidPayloadException("Payload does not match the job type"));

        mvc.perform(postJob("""
                {"type":"ECHO","payload":{"x":1}}
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("INVALID_PAYLOAD"));
    }

    // ==========================================================
    // POST /jobs : errors thrown by the service
    // These also prove the advice ORDER: a specific handler must win over the 500 catch-all.
    // ==========================================================

    // @Test
    void post_queueFull_returnsQueueFullStatusNot500() throws Exception {
        when(jobService.submit(any())).thenThrow(new QueueFullException());

        mvc.perform(postJob(VALID_BODY))
            .andExpect(status().is(QUEUE_FULL_HTTP_STATUS))
            .andExpect(jsonPath("$.errorCode").value("QUEUE_FULL"))
            .andExpect(jsonPath("$.success").value(false));
    }

    // @Test
    void post_executorShutdown_returns503() throws Exception {
        when(jobService.submit(any())).thenThrow(new ExecutorShutdownException());

        mvc.perform(postJob(VALID_BODY))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.errorCode").value("EXECUTOR_SHUTDOWN"));
    }

    // @Test
    void post_duplicateJob_returns409() throws Exception {
        when(jobService.submit(any())).thenThrow(new DuplicateJobException(UUID.randomUUID()));

        mvc.perform(postJob(VALID_BODY))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.errorCode").value("CONFLICT"));
    }

    // @Test
    void post_unexpectedError_returns500WithInternalErrorCode() throws Exception {
        when(jobService.submit(any())).thenThrow(new IllegalStateException("boom"));

        mvc.perform(postJob(VALID_BODY))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.errorCode").value("INTERNAL_SERVER_ERROR"))
            .andExpect(jsonPath("$.success").value(false));
    }

    // ==========================================================
    // GET /jobs/{id}
    // ==========================================================

    // @Test
    void get_existingJob_returns200WithJob() throws Exception {
        Job running = job(JobStatus.RUNNING);
        when(jobService.getJob(eq(running.id()))).thenReturn(running);

        mvc.perform(get("/api/jobs/{id}", running.id()))
            .andExpect(status().isOk())
            .andExpect(jsonPath(ID_PATH).value(running.id().toString()))
            .andExpect(jsonPath(STATUS_PATH).value("RUNNING"))
            .andExpect(jsonPath(TYPE_PATH).value("ECHO"));
    }

    // @Test
    void get_unknownJob_returns404WithJobNotFoundCode() throws Exception {
        UUID id = UUID.randomUUID();
        when(jobService.getJob(eq(id))).thenThrow(new JobNotFoundException(id));

        mvc.perform(get("/api/jobs/{id}", id))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errorCode").value("JOB_NOT_FOUND"))
            .andExpect(jsonPath("$.success").value(false));
    }

    // @Test
    void get_idIsNotAUuid_returns400Not500() throws Exception {
        mvc.perform(get("/api/jobs/{id}", "not-a-uuid"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"));

        verifyNoInteractions(jobService);
    }
}