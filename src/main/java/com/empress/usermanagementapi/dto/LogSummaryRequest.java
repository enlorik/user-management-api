package com.empress.usermanagementapi.dto;

import lombok.Data;

/**
 * Request DTO for log summarization endpoint.
 * Contains optional filtering parameters for log analysis.
 */
@Data
public class LogSummaryRequest {
    /**
     * Start time for log filtering in ISO8601 format.
     * Example: "2026-01-10T00:00:00Z"
     */
    private String startTime;
    
    /**
     * End time for log filtering in ISO8601 format.
     * Example: "2026-01-10T23:59:59Z"
     */
    private String endTime;
    
    /**
     * Log level filter (INFO, WARN, ERROR, DEBUG).
     */
    private String logLevel;
    
    /**
     * Action type filter (USER_LOGIN, USER_REGISTRATION, etc.).
     */
    private String actionType;
    
    /**
     * User ID filter for user-specific log analysis.
     */
    private String userId;
}
