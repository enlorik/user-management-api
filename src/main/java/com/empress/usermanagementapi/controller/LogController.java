package com.empress.usermanagementapi.controller;

import com.empress.usermanagementapi.dto.LogEntry;
import com.empress.usermanagementapi.dto.LogSummaryRequest;
import com.empress.usermanagementapi.dto.LogSummaryResponse;
import com.empress.usermanagementapi.service.LogReaderService;
import com.empress.usermanagementapi.service.LogSanitizerService;
import com.empress.usermanagementapi.service.LogSummarizerService;
import com.empress.usermanagementapi.util.LoggingUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for log analysis and summarization.
 * Provides AI-powered insights from system logs.
 */
@RestController
@RequestMapping("/api/v1/logs")
@Tag(name = "Log Analysis", description = "AI-powered log summarization and analysis endpoints")
public class LogController {
    
    private static final Logger log = LoggerFactory.getLogger(LogController.class);
    
    private final LogReaderService logReaderService;
    private final LogSanitizerService logSanitizerService;
    private final LogSummarizerService logSummarizerService;
    
    public LogController(LogReaderService logReaderService,
                        LogSanitizerService logSanitizerService,
                        LogSummarizerService logSummarizerService) {
        this.logReaderService = logReaderService;
        this.logSanitizerService = logSanitizerService;
        this.logSummarizerService = logSummarizerService;
    }
    
    /**
     * Summarize logs within a specified time range with optional filters.
     * 
     * @param startTime Start time in ISO8601 format (optional)
     * @param endTime End time in ISO8601 format (optional)
     * @param logLevel Log level filter (INFO, WARN, ERROR, DEBUG) (optional)
     * @param actionType Action type filter (USER_LOGIN, USER_REGISTRATION, etc.) (optional)
     * @param userId User ID filter (optional)
     * @return Log summary with AI-generated insights
     */
    @GetMapping("/summarize")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Summarize system logs",
        description = "Analyze and summarize system logs with AI-powered insights. Supports filtering by time range, log level, action type, and user ID.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Successfully generated log summary",
                content = @Content(schema = @Schema(implementation = LogSummaryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions (admin role required)")
        }
    )
    public ResponseEntity<?> summarizeLogs(
            @Parameter(description = "Start time in ISO8601 format (e.g., 2026-01-10T00:00:00Z)")
            @RequestParam(required = false) String startTime,
            
            @Parameter(description = "End time in ISO8601 format (e.g., 2026-01-10T23:59:59Z)")
            @RequestParam(required = false) String endTime,
            
            @Parameter(description = "Log level filter (INFO, WARN, ERROR, DEBUG)")
            @RequestParam(required = false) String logLevel,
            
            @Parameter(description = "Action type filter (e.g., USER_LOGIN, USER_REGISTRATION)")
            @RequestParam(required = false) String actionType,
            
            @Parameter(description = "User ID filter")
            @RequestParam(required = false) String userId) {
        
        LoggingUtil.setActionType("LOG_SUMMARIZATION");
        log.info("Received log summarization request - startTime: {}, endTime: {}, logLevel: {}, actionType: {}, userId: {}",
                startTime, endTime, logLevel, actionType, userId);
        
        try {
            // Parse and validate time parameters
            Instant start = parseTimestamp(startTime, "startTime");
            Instant end = parseTimestamp(endTime, "endTime");
            
            // Validate time range
            if (start != null && end != null && start.isAfter(end)) {
                log.warn("Invalid time range - start time is after end time");
                LoggingUtil.clearActionType();
                return ResponseEntity
                        .badRequest()
                        .body(Map.of("error", "Invalid time range: startTime must be before endTime"));
            }
            
            // Read logs from file system
            List<LogEntry> logEntries = logReaderService.readLogs(start, end);
            log.debug("Read {} log entries from file system", logEntries.size());
            
            // Apply filters
            logEntries = applyFilters(logEntries, logLevel, actionType, userId);
            log.debug("After filtering: {} log entries", logEntries.size());
            
            // Check if any logs found
            if (logEntries.isEmpty()) {
                log.info("No logs found matching the criteria");
                LoggingUtil.clearActionType();
                return ResponseEntity.ok(LogSummaryResponse.builder()
                        .summary("No logs found matching the specified criteria.")
                        .totalLogs(0)
                        .startTime(start != null ? start.toString() : null)
                        .endTime(end != null ? end.toString() : null)
                        .logLevelStats(Map.of())
                        .actionTypeStats(Map.of())
                        .topIssues(List.of())
                        .build());
            }
            
            // Sanitize logs before processing
            List<LogEntry> sanitizedLogs = logSanitizerService.sanitizeLogs(logEntries);
            log.debug("Sanitized {} log entries", sanitizedLogs.size());
            
            // Generate summary
            LogSummaryResponse response = logSummarizerService.summarizeLogs(sanitizedLogs, start, end);
            log.info("Generated log summary - totalLogs: {}, errors: {}, warnings: {}",
                    response.getTotalLogs(),
                    response.getLogLevelStats().getOrDefault("ERROR", 0),
                    response.getLogLevelStats().getOrDefault("WARN", 0));
            
            LoggingUtil.clearActionType();
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid request parameters: {}", e.getMessage());
            LoggingUtil.clearActionType();
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error generating log summary", e);
            LoggingUtil.clearActionType();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An error occurred while generating log summary"));
        }
    }
    
    /**
     * Parse ISO8601 timestamp string.
     * 
     * @param timestampStr Timestamp string to parse
     * @param paramName Parameter name for error messages
     * @return Parsed Instant or null if string is null/empty
     * @throws IllegalArgumentException if timestamp format is invalid
     */
    private Instant parseTimestamp(String timestampStr, String paramName) {
        if (timestampStr == null || timestampStr.trim().isEmpty()) {
            return null;
        }
        
        try {
            return Instant.parse(timestampStr);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Invalid " + paramName + " format. Expected ISO8601 format (e.g., 2026-01-10T00:00:00Z)");
        }
    }
    
    /**
     * Apply filters to log entries.
     * 
     * @param logEntries List of log entries to filter
     * @param logLevel Log level filter (can be null)
     * @param actionType Action type filter (can be null)
     * @param userId User ID filter (can be null)
     * @return Filtered list of log entries
     */
    private List<LogEntry> applyFilters(List<LogEntry> logEntries, String logLevel, 
                                       String actionType, String userId) {
        return logEntries.stream()
                .filter(entry -> logLevel == null || logLevel.equalsIgnoreCase(entry.getLevel()))
                .filter(entry -> actionType == null || actionType.equals(entry.getActionType()))
                .filter(entry -> userId == null || userId.equals(entry.getUserId()))
                .collect(Collectors.toList());
    }
}
