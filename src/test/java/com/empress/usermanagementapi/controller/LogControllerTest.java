package com.empress.usermanagementapi.controller;

import com.empress.usermanagementapi.dto.LogEntry;
import com.empress.usermanagementapi.dto.LogSummaryResponse;
import com.empress.usermanagementapi.service.LogReaderService;
import com.empress.usermanagementapi.service.LogSanitizerService;
import com.empress.usermanagementapi.service.LogSummarizerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

/**
 * Integration tests for LogController.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LogReaderService logReaderService;

    @MockBean
    private LogSanitizerService logSanitizerService;

    @MockBean
    private LogSummarizerService logSummarizerService;

    private List<LogEntry> sampleLogEntries;
    private LogSummaryResponse sampleResponse;

    @BeforeEach
    void setUp() {
        // Create sample log entries
        sampleLogEntries = createSampleLogEntries();
        
        // Create sample response
        sampleResponse = LogSummaryResponse.builder()
                .summary("Test summary")
                .totalLogs(5)
                .startTime(Instant.now().minusSeconds(3600).toString())
                .endTime(Instant.now().toString())
                .logLevelStats(Map.of("INFO", 3, "ERROR", 2))
                .actionTypeStats(Map.of("USER_LOGIN", 2, "USER_CREATE", 3))
                .topIssues(Arrays.asList("Login failure (occurred 2 times)"))
                .build();
    }

    private List<LogEntry> createSampleLogEntries() {
        List<LogEntry> entries = new ArrayList<>();
        Instant now = Instant.now();
        
        for (int i = 0; i < 5; i++) {
            LogEntry entry = new LogEntry();
            entry.setTimestamp(now.minusSeconds(i * 60));
            entry.setLevel(i % 2 == 0 ? "INFO" : "ERROR");
            entry.setLogger("com.empress.usermanagementapi.TestLogger");
            entry.setMessage("Test log message " + i);
            entry.setActionType(i % 2 == 0 ? "USER_CREATE" : "USER_LOGIN");
            entry.setUserId(String.valueOf(i + 1));
            entries.add(entry);
        }
        
        return entries;
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testSummarizeLogs_WithAdminRole_ReturnsSuccess() throws Exception {
        // Arrange
        when(logReaderService.readLogs(any(), any())).thenReturn(sampleLogEntries);
        when(logSanitizerService.sanitizeLogs(any())).thenReturn(sampleLogEntries);
        when(logSummarizerService.summarizeLogs(any(), any(), any())).thenReturn(sampleResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/logs/summarize"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("Test summary"))
                .andExpect(jsonPath("$.totalLogs").value(5))
                .andExpect(jsonPath("$.logLevelStats.INFO").value(3))
                .andExpect(jsonPath("$.logLevelStats.ERROR").value(2));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testSummarizeLogs_WithUserRole_ReturnsForbidden() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/logs/summarize"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testSummarizeLogs_WithoutAuthentication_RedirectsToLogin() throws Exception {
        // Act & Assert
        // Spring Security redirects to login page (302) when not authenticated
        mockMvc.perform(get("/api/v1/logs/summarize"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "http://localhost/login"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testSummarizeLogs_WithTimeRange_ReturnsSuccess() throws Exception {
        // Arrange
        when(logReaderService.readLogs(any(), any())).thenReturn(sampleLogEntries);
        when(logSanitizerService.sanitizeLogs(any())).thenReturn(sampleLogEntries);
        when(logSummarizerService.summarizeLogs(any(), any(), any())).thenReturn(sampleResponse);

        // Use relative dates for more maintainable tests
        Instant now = Instant.now();
        String startTime = now.minus(java.time.Duration.ofHours(1)).toString();
        String endTime = now.toString();

        // Act & Assert
        mockMvc.perform(get("/api/v1/logs/summarize")
                        .param("startTime", startTime)
                        .param("endTime", endTime))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testSummarizeLogs_WithInvalidTimeFormat_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/logs/summarize")
                        .param("startTime", "invalid-date"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("Invalid startTime format")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testSummarizeLogs_WithStartTimeAfterEndTime_ReturnsBadRequest() throws Exception {
        // Use relative dates for more maintainable tests
        Instant now = Instant.now();
        String startTime = now.toString();
        String endTime = now.minus(java.time.Duration.ofHours(1)).toString();
        
        // Act & Assert
        mockMvc.perform(get("/api/v1/logs/summarize")
                        .param("startTime", startTime)
                        .param("endTime", endTime))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("startTime must be before endTime")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testSummarizeLogs_WithLogLevelFilter_ReturnsSuccess() throws Exception {
        // Arrange
        List<LogEntry> errorLogs = sampleLogEntries.stream()
                .filter(entry -> "ERROR".equals(entry.getLevel()))
                .toList();
        
        when(logReaderService.readLogs(any(), any())).thenReturn(sampleLogEntries);
        when(logSanitizerService.sanitizeLogs(any())).thenReturn(errorLogs);
        when(logSummarizerService.summarizeLogs(any(), any(), any())).thenReturn(sampleResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/logs/summarize")
                        .param("logLevel", "ERROR"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testSummarizeLogs_WithActionTypeFilter_ReturnsSuccess() throws Exception {
        // Arrange
        when(logReaderService.readLogs(any(), any())).thenReturn(sampleLogEntries);
        when(logSanitizerService.sanitizeLogs(any())).thenReturn(sampleLogEntries);
        when(logSummarizerService.summarizeLogs(any(), any(), any())).thenReturn(sampleResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/logs/summarize")
                        .param("actionType", "USER_LOGIN"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testSummarizeLogs_WithUserIdFilter_ReturnsSuccess() throws Exception {
        // Arrange
        when(logReaderService.readLogs(any(), any())).thenReturn(sampleLogEntries);
        when(logSanitizerService.sanitizeLogs(any())).thenReturn(sampleLogEntries);
        when(logSummarizerService.summarizeLogs(any(), any(), any())).thenReturn(sampleResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/logs/summarize")
                        .param("userId", "123"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testSummarizeLogs_WithMultipleFilters_ReturnsSuccess() throws Exception {
        // Arrange
        when(logReaderService.readLogs(any(), any())).thenReturn(sampleLogEntries);
        when(logSanitizerService.sanitizeLogs(any())).thenReturn(sampleLogEntries);
        when(logSummarizerService.summarizeLogs(any(), any(), any())).thenReturn(sampleResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/logs/summarize")
                        .param("logLevel", "ERROR")
                        .param("actionType", "USER_LOGIN")
                        .param("userId", "1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testSummarizeLogs_NoLogsFound_ReturnsEmptyResponse() throws Exception {
        // Arrange
        when(logReaderService.readLogs(any(), any())).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/api/v1/logs/summarize"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("No logs found matching the specified criteria."))
                .andExpect(jsonPath("$.totalLogs").value(0));
    }
}
