package com.empress.usermanagementapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Response DTO for log summarization endpoint.
 * Contains AI-generated summary and statistics about the analyzed logs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogSummaryResponse {
    /**
     * AI-generated summary of the logs.
     */
    private String summary;
    
    /**
     * Total number of logs analyzed.
     */
    private int totalLogs;
    
    /**
     * Start time of the analyzed period.
     */
    private String startTime;
    
    /**
     * End time of the analyzed period.
     */
    private String endTime;
    
    /**
     * Statistics by log level (INFO, WARN, ERROR, etc.).
     */
    private Map<String, Integer> logLevelStats;
    
    /**
     * Statistics by action type.
     */
    private Map<String, Integer> actionTypeStats;
    
    /**
     * Most common errors or issues found.
     */
    private java.util.List<String> topIssues;
}
