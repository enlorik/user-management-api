package com.empress.usermanagementapi.service;

import com.empress.usermanagementapi.dto.LogEntry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Service for reading and parsing log files.
 * Supports JSON-formatted logs from Logback with Logstash encoder.
 */
@Service
public class LogReaderService {
    
    private static final Logger log = LoggerFactory.getLogger(LogReaderService.class);
    private static final String LOG_DIRECTORY = "logs";
    private final ObjectMapper objectMapper;
    
    public LogReaderService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    /**
     * Read and parse log files within a time range.
     * 
     * @param startTime Start time for filtering (can be null for no lower bound)
     * @param endTime End time for filtering (can be null for no upper bound)
     * @return List of parsed log entries
     */
    public List<LogEntry> readLogs(Instant startTime, Instant endTime) {
        List<LogEntry> logEntries = new ArrayList<>();
        
        // Check if log directory exists
        File logDir = new File(LOG_DIRECTORY);
        if (!logDir.exists() || !logDir.isDirectory()) {
            log.warn("Log directory does not exist: {}", LOG_DIRECTORY);
            return logEntries;
        }
        
        // Get all log files
        File[] logFiles = logDir.listFiles((dir, name) -> name.endsWith(".log"));
        if (logFiles == null || logFiles.length == 0) {
            log.info("No log files found in directory: {}", LOG_DIRECTORY);
            return logEntries;
        }
        
        // Read each log file
        for (File logFile : logFiles) {
            try {
                List<LogEntry> entries = parseLogFile(logFile.toPath(), startTime, endTime);
                logEntries.addAll(entries);
                log.debug("Read {} entries from log file: {}", entries.size(), logFile.getName());
            } catch (IOException e) {
                log.error("Error reading log file: {}", logFile.getName(), e);
            }
        }
        
        log.info("Total log entries read: {}", logEntries.size());
        return logEntries;
    }
    
    /**
     * Parse a single log file and extract log entries.
     * 
     * @param logFilePath Path to the log file
     * @param startTime Start time filter (can be null)
     * @param endTime End time filter (can be null)
     * @return List of parsed log entries
     * @throws IOException If file reading fails
     */
    private List<LogEntry> parseLogFile(Path logFilePath, Instant startTime, Instant endTime) throws IOException {
        List<LogEntry> entries = new ArrayList<>();
        
        try (Stream<String> lines = Files.lines(logFilePath)) {
            lines.forEach(line -> {
                try {
                    LogEntry entry = parseLogLine(line);
                    if (entry != null && isWithinTimeRange(entry, startTime, endTime)) {
                        entries.add(entry);
                    }
                } catch (Exception e) {
                    log.debug("Failed to parse log line: {}", line, e);
                }
            });
        }
        
        return entries;
    }
    
    /**
     * Parse a single log line (JSON format).
     * 
     * @param line JSON log line
     * @return Parsed LogEntry or null if parsing fails
     */
    private LogEntry parseLogLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }
        
        try {
            JsonNode jsonNode = objectMapper.readTree(line);
            
            LogEntry entry = new LogEntry();
            entry.setTimestamp(parseTimestamp(jsonNode.path("timestamp").asText()));
            entry.setLevel(jsonNode.path("level").asText(null));
            entry.setLogger(jsonNode.path("logger").asText(null));
            entry.setMessage(jsonNode.path("message").asText(null));
            entry.setThread(jsonNode.path("thread").asText(null));
            entry.setRequestId(jsonNode.path("requestId").asText(null));
            entry.setUserId(jsonNode.path("userId").asText(null));
            entry.setActionType(jsonNode.path("actionType").asText(null));
            entry.setHttpStatus(jsonNode.path("httpStatus").asText(null));
            entry.setApplication(jsonNode.path("application").asText(null));
            
            return entry;
        } catch (Exception e) {
            log.debug("Failed to parse JSON log line", e);
            return null;
        }
    }
    
    /**
     * Parse timestamp from ISO8601 string.
     * 
     * @param timestampStr Timestamp string
     * @return Parsed Instant or null if parsing fails
     */
    private Instant parseTimestamp(String timestampStr) {
        if (timestampStr == null || timestampStr.isEmpty()) {
            return null;
        }
        
        try {
            return Instant.parse(timestampStr);
        } catch (DateTimeParseException e) {
            log.debug("Failed to parse timestamp: {}", timestampStr);
            return null;
        }
    }
    
    /**
     * Check if log entry is within the specified time range.
     * 
     * @param entry Log entry to check
     * @param startTime Start time (can be null)
     * @param endTime End time (can be null)
     * @return true if entry is within range, false otherwise
     */
    private boolean isWithinTimeRange(LogEntry entry, Instant startTime, Instant endTime) {
        if (entry.getTimestamp() == null) {
            return false;
        }
        
        if (startTime != null && entry.getTimestamp().isBefore(startTime)) {
            return false;
        }
        
        if (endTime != null && entry.getTimestamp().isAfter(endTime)) {
            return false;
        }
        
        return true;
    }
}
