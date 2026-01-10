package com.empress.usermanagementapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Represents a single log entry from the system logs.
 * Used for parsing and processing log files.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogEntry {
    private Instant timestamp;
    private String level;
    private String logger;
    private String message;
    private String thread;
    private String requestId;
    private String userId;
    private String actionType;
    private String httpStatus;
    private String application;
}
