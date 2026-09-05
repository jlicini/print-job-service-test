package com.adobe.printservice.web;

import com.adobe.printservice.model.Job;
import com.adobe.printservice.model.JobStatus;
import com.adobe.printservice.repository.JobRepository;
import com.adobe.printservice.worker.JobWorker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class JobResourceTest {

    private static final String INVOICE_TEMPLATE_ID = "b6f1e6a2-6b8b-4a9d-9c2e-3f2d8a2f9b10";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JobRepository jobRepository;

    @MockitoBean
    private JobWorker jobWorker;

    private Job queuedJob;
    private Job failedJob;
    private Job doneJob;

    @BeforeEach
    void setUpJobs() {
        jobRepository.deleteAll();

        queuedJob = new Job();
        queuedJob.setTemplateId(INVOICE_TEMPLATE_ID);
        queuedJob.setParameters(Map.of());
        queuedJob.setStatus(JobStatus.QUEUED);

        failedJob = new Job();
        failedJob.setTemplateId(INVOICE_TEMPLATE_ID);
        failedJob.setParameters(Map.of());
        failedJob.setStatus(JobStatus.FAILED);
        failedJob.setAttempts(3);
        failedJob.setErrorMessage("Simulated render failure");

        doneJob = new Job();
        doneJob.setTemplateId(INVOICE_TEMPLATE_ID);
        doneJob.setParameters(Map.of());
        doneJob.setStatus(JobStatus.DONE);
        doneJob.setResultContent("Rendered output");

        jobRepository.saveAllAndFlush(List.of(queuedJob, failedJob, doneJob));
    }

    @Test
    void createJob_knownTemplate_returns201() throws Exception {
        mockMvc.perform(post("/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateId": "%s",
                                  "parameters": {"customer": "Adobe"}
                                }
                                """.formatted(INVOICE_TEMPLATE_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.templateId").value(INVOICE_TEMPLATE_ID))
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andExpect(jsonPath("$.attempts").value(0))
                .andExpect(jsonPath("$.resultAvailable").value(false))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    void createJob_unknownTemplate_returns400() throws Exception {
        mockMvc.perform(post("/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateId": "does-not-exist",
                                  "parameters": {}
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Template does not exist: does-not-exist"));
    }

    @Test
    void createJob_missingTemplateId_returns400() throws Exception {
        mockMvc.perform(post("/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "parameters": {}
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getJobs_failedFilter_returns200() throws Exception {
        mockMvc.perform(get("/jobs").param("status", "FAILED"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(failedJob.getId()))
                .andExpect(jsonPath("$[0].status").value("FAILED"))
                .andExpect(jsonPath("$[0].templateId").value(INVOICE_TEMPLATE_ID));
    }

    @Test
    void getJob_existingJob_returns200() throws Exception {
        mockMvc.perform(get("/jobs/{id}", queuedJob.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(queuedJob.getId()))
                .andExpect(jsonPath("$.status").value("QUEUED"));
    }

    @Test
    void getJob_unknownId_returns404() throws Exception {
        mockMvc.perform(get("/jobs/{id}", "missing-id"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Job does not exist: missing-id"));
    }

    @Test
    void getJobResult_doneJob_returns200() throws Exception {
        mockMvc.perform(get("/jobs/{id}/result", doneJob.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string("Rendered output"));
    }

    @Test
    void getJobResult_queuedJob_returns409() throws Exception {
        mockMvc.perform(get("/jobs/{id}/result", queuedJob.getId()))
                .andExpect(status().isConflict())
                .andExpect(content().string(
                        "Job result is not available while job %s is in QUEUED state"
                                .formatted(queuedJob.getId())
                ));
    }

}
