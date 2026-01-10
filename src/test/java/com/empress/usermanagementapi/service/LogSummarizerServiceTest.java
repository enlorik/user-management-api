package com.empress.usermanagementapi.service;

import com.empress.usermanagementapi.dto.LogEntry;
import com.empress.usermanagementapi.dto.LogSummaryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LogSummarizerService.
 */
@ExtendWith(MockitoExtension.class)
class LogSummarizerServiceTest {

    @InjectMocks
    private LogSummarizerService logSummarizerService;

    private List<LogEntry> sampleLogEntries;

    @BeforeEach
    void setUp() {
        sampleLogEntries = createSampleLogEntries();
    }

    private List<LogEntry> createSampleLogEntries() {
        List<LogEntry> entries = new ArrayList<>();
        Instant now = Instant.now();
        
        // Create INFO logs
        for (int i = 0; i < 3; i++) {
            LogEntry entry = new LogEntry();
            entry.setTimestamp(now.minusSeconds(i * 60));
            entry.setLevel("INFO");
            entry.setLogger("com.empress.test");
            entry.setMessage("User logged in successfully");
            entry.setActionType("USER_LOGIN");
            entry.setUserId(String.valueOf(i + 1));
            entries.add(entry);
        }
        
        // Create ERROR logs
        for (int i = 0; i < 2; i++) {
            LogEntry entry = new LogEntry();
            entry.setTimestamp(now.minusSeconds((i + 3) * 60));
            entry.setLevel("ERROR");
            entry.setLogger("com.empress.test");
            entry.setMessage("Login failed for user");
            entry.setActionType("USER_LOGIN");
            entry.setUserId(String.valueOf(i + 1));
            entries.add(entry);
        }
        
        return entries;
    }

    @Test
    void testSummarizeLogs_GeneratesBasicSummary() {
        // Arrange
        Instant start = Instant.now().minusSeconds(3600);
        Instant end = Instant.now();

        // Act
        LogSummaryResponse response = logSummarizerService.summarizeLogs(sampleLogEntries, start, end);

        // Assert
        assertNotNull(response);
        assertEquals(5, response.getTotalLogs());
        assertNotNull(response.getSummary());
        assertFalse(response.getSummary().isEmpty());
    }

    @Test
    void testSummarizeLogs_CalculatesLogLevelStats() {
        // Arrange
        Instant start = Instant.now().minusSeconds(3600);
        Instant end = Instant.now();

        // Act
        LogSummaryResponse response = logSummarizerService.summarizeLogs(sampleLogEntries, start, end);

        // Assert
        assertNotNull(response.getLogLevelStats());
        assertEquals(3, response.getLogLevelStats().get("INFO"));
        assertEquals(2, response.getLogLevelStats().get("ERROR"));
    }

    @Test
    void testSummarizeLogs_CalculatesActionTypeStats() {
        // Arrange
        Instant start = Instant.now().minusSeconds(3600);
        Instant end = Instant.now();

        // Act
        LogSummaryResponse response = logSummarizerService.summarizeLogs(sampleLogEntries, start, end);

        // Assert
        assertNotNull(response.getActionTypeStats());
        assertEquals(5, response.getActionTypeStats().get("USER_LOGIN"));
    }

    @Test
    void testSummarizeLogs_IdentifiesTopIssues() {
        // Arrange
        Instant start = Instant.now().minusSeconds(3600);
        Instant end = Instant.now();

        // Act
        LogSummaryResponse response = logSummarizerService.summarizeLogs(sampleLogEntries, start, end);

        // Assert
        assertNotNull(response.getTopIssues());
        assertFalse(response.getTopIssues().isEmpty());
        // Check if any issue contains "failure" or "occurred"
        boolean hasFailureIssue = response.getTopIssues().stream()
                .anyMatch(issue -> issue.toLowerCase().contains("failure") || issue.toLowerCase().contains("occurred"));
        assertTrue(hasFailureIssue, "Should have at least one issue identified");
    }

    @Test
    void testSummarizeLogs_HandlesEmptyList() {
        // Arrange
        List<LogEntry> emptyList = new ArrayList<>();
        Instant start = Instant.now().minusSeconds(3600);
        Instant end = Instant.now();

        // Act
        LogSummaryResponse response = logSummarizerService.summarizeLogs(emptyList, start, end);

        // Assert
        assertNotNull(response);
        assertEquals(0, response.getTotalLogs());
        assertNotNull(response.getSummary());
    }

    @Test
    void testSummarizeLogs_IncludesTimePeriod() {
        // Arrange
        Instant start = Instant.now().minusSeconds(3600);
        Instant end = Instant.now();

        // Act
        LogSummaryResponse response = logSummarizerService.summarizeLogs(sampleLogEntries, start, end);

        // Assert
        assertNotNull(response.getStartTime());
        assertNotNull(response.getEndTime());
        assertEquals(start.toString(), response.getStartTime());
        assertEquals(end.toString(), response.getEndTime());
    }

    @Test
    void testSummarizeLogs_SummaryContainsKeyInformation() {
        // Arrange
        Instant start = Instant.now().minusSeconds(3600);
        Instant end = Instant.now();

        // Act
        LogSummaryResponse response = logSummarizerService.summarizeLogs(sampleLogEntries, start, end);

        // Assert
        String summary = response.getSummary();
        assertTrue(summary.contains("Total Events"));
        assertTrue(summary.contains("Log Level Distribution"));
        assertTrue(summary.contains("errors detected"));
    }

    @Test
    void testSummarizeLogs_WithOnlyInfoLogs() {
        // Arrange
        List<LogEntry> infoOnlyLogs = new ArrayList<>();
        Instant now = Instant.now();
        
        for (int i = 0; i < 5; i++) {
            LogEntry entry = new LogEntry();
            entry.setTimestamp(now.minusSeconds(i * 60));
            entry.setLevel("INFO");
            entry.setMessage("Test info message");
            infoOnlyLogs.add(entry);
        }
        
        Instant start = Instant.now().minusSeconds(3600);
        Instant end = Instant.now();

        // Act
        LogSummaryResponse response = logSummarizerService.summarizeLogs(infoOnlyLogs, start, end);

        // Assert
        assertNotNull(response);
        assertEquals(5, response.getTotalLogs());
        assertEquals(5, response.getLogLevelStats().get("INFO"));
        assertFalse(response.getLogLevelStats().containsKey("ERROR"));
        assertTrue(response.getSummary().contains("No errors detected"));
    }

    @Test
    void testSummarizeLogs_WithHighErrorRate() {
        // Arrange
        List<LogEntry> highErrorLogs = new ArrayList<>();
        Instant now = Instant.now();
        
        // 8 errors out of 10 logs (80% error rate)
        for (int i = 0; i < 8; i++) {
            LogEntry entry = new LogEntry();
            entry.setTimestamp(now.minusSeconds(i * 60));
            entry.setLevel("ERROR");
            entry.setMessage("Error occurred");
            highErrorLogs.add(entry);
        }
        
        for (int i = 0; i < 2; i++) {
            LogEntry entry = new LogEntry();
            entry.setTimestamp(now.minusSeconds((i + 8) * 60));
            entry.setLevel("INFO");
            entry.setMessage("Info message");
            highErrorLogs.add(entry);
        }
        
        Instant start = Instant.now().minusSeconds(3600);
        Instant end = Instant.now();

        // Act
        LogSummaryResponse response = logSummarizerService.summarizeLogs(highErrorLogs, start, end);

        // Assert
        assertNotNull(response);
        assertTrue(response.getSummary().contains("High error rate"));
    }
}
