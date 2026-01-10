package com.empress.usermanagementapi.service;

import com.empress.usermanagementapi.dto.LogEntry;
import com.empress.usermanagementapi.util.LoggingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for sanitizing log entries before sending to AI summarization.
 * Uses existing masking utilities to remove sensitive data.
 */
@Service
public class LogSanitizerService {
    
    private static final Logger log = LoggerFactory.getLogger(LogSanitizerService.class);
    
    // Patterns for detecting sensitive data
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b\\d{3}[-.]?\\d{3}[-.]?\\d{4}\\b");
    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\b[a-fA-F0-9]{32,}\\b");
    private static final Pattern SSN_PATTERN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");
    
    /**
     * Sanitize a list of log entries by masking sensitive information.
     * 
     * @param logEntries List of log entries to sanitize
     * @return List of sanitized log entries
     */
    public List<LogEntry> sanitizeLogs(List<LogEntry> logEntries) {
        log.debug("Sanitizing {} log entries", logEntries.size());
        
        return logEntries.stream()
                .map(this::sanitizeLogEntry)
                .collect(Collectors.toList());
    }
    
    /**
     * Sanitize a single log entry.
     * 
     * @param entry Log entry to sanitize
     * @return Sanitized log entry
     */
    private LogEntry sanitizeLogEntry(LogEntry entry) {
        LogEntry sanitized = new LogEntry();
        
        // Copy non-sensitive fields directly
        sanitized.setTimestamp(entry.getTimestamp());
        sanitized.setLevel(entry.getLevel());
        sanitized.setLogger(entry.getLogger());
        sanitized.setThread(entry.getThread());
        sanitized.setRequestId(entry.getRequestId());
        sanitized.setActionType(entry.getActionType());
        sanitized.setHttpStatus(entry.getHttpStatus());
        sanitized.setApplication(entry.getApplication());
        
        // Sanitize message (most likely to contain sensitive data)
        sanitized.setMessage(sanitizeMessage(entry.getMessage()));
        
        // Mask userId if present (partial masking for analysis)
        sanitized.setUserId(maskUserId(entry.getUserId()));
        
        return sanitized;
    }
    
    /**
     * Sanitize log message by masking sensitive data.
     * 
     * @param message Original message
     * @return Sanitized message
     */
    private String sanitizeMessage(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        
        String sanitized = message;
        
        // Mask email addresses
        sanitized = maskEmails(sanitized);
        
        // Mask phone numbers
        sanitized = maskPhoneNumbers(sanitized);
        
        // Mask tokens and API keys
        sanitized = maskTokens(sanitized);
        
        // Mask SSN
        sanitized = maskSSN(sanitized);
        
        return sanitized;
    }
    
    /**
     * Mask email addresses in text.
     * 
     * @param text Text to process
     * @return Text with masked emails
     */
    private String maskEmails(String text) {
        Matcher matcher = EMAIL_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String email = matcher.group();
            String masked = LoggingUtil.maskEmail(email);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(masked));
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }
    
    /**
     * Mask phone numbers in text.
     * 
     * @param text Text to process
     * @return Text with masked phone numbers
     */
    private String maskPhoneNumbers(String text) {
        return PHONE_PATTERN.matcher(text).replaceAll("***-***-****");
    }
    
    /**
     * Mask tokens and API keys in text.
     * 
     * @param text Text to process
     * @return Text with masked tokens
     */
    private String maskTokens(String text) {
        return TOKEN_PATTERN.matcher(text).replaceAll("****MASKED_TOKEN****");
    }
    
    /**
     * Mask SSN in text.
     * 
     * @param text Text to process
     * @return Text with masked SSN
     */
    private String maskSSN(String text) {
        return SSN_PATTERN.matcher(text).replaceAll("***-**-****");
    }
    
    /**
     * Mask user ID (partial masking for analysis purposes).
     * 
     * @param userId User ID to mask
     * @return Partially masked user ID
     */
    private String maskUserId(String userId) {
        if (userId == null || userId.isEmpty()) {
            return userId;
        }
        
        // For analysis purposes, we keep user IDs but could implement partial masking
        // if required for stricter privacy
        return userId;
    }
}
