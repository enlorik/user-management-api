package com.empress.usermanagementapi.util;

import org.slf4j.Logger;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * Utility class for enhanced structured logging with metadata support.
 * 
 * This class provides helper methods to add contextual metadata to log entries
 * using SLF4J's Mapped Diagnostic Context (MDC). The metadata is automatically
 * included in JSON log output and helps with:
 * - Tracing requests across services
 * - AI-powered log analysis and summarization
 * - Filtering and searching logs by specific actions or users
 * - Correlating related log entries
 * 
 * Standard metadata fields:
 * - requestId: Unique identifier for each request
 * - userId: User identifier for user-specific actions
 * - actionType: Type of action being performed (e.g., USER_LOGIN, USER_REGISTRATION)
 * - httpStatus: HTTP status code for the response
 */
public class LoggingUtil {
    
    // MDC key constants
    public static final String REQUEST_ID = "requestId";
    public static final String USER_ID = "userId";
    public static final String ACTION_TYPE = "actionType";
    public static final String HTTP_STATUS = "httpStatus";
    
    /**
     * Generate a new unique request ID and add it to MDC.
     * Should be called at the start of each request.
     * 
     * @return The generated request ID
     */
    public static String generateRequestId() {
        String requestId = UUID.randomUUID().toString();
        MDC.put(REQUEST_ID, requestId);
        return requestId;
    }
    
    /**
     * Set the request ID in MDC.
     * 
     * @param requestId The request ID to set
     */
    public static void setRequestId(String requestId) {
        if (requestId != null && !requestId.isEmpty()) {
            MDC.put(REQUEST_ID, requestId);
        }
    }
    
    /**
     * Set the user ID in MDC.
     * 
     * @param userId The user ID to set
     */
    public static void setUserId(String userId) {
        if (userId != null && !userId.isEmpty()) {
            MDC.put(USER_ID, userId);
        }
    }
    
    /**
     * Set the user ID in MDC.
     * 
     * @param userId The user ID to set
     */
    public static void setUserId(Long userId) {
        if (userId != null) {
            MDC.put(USER_ID, userId.toString());
        }
    }
    
    /**
     * Set the action type in MDC.
     * 
     * @param actionType The action type to set (e.g., USER_LOGIN, USER_REGISTRATION)
     */
    public static void setActionType(String actionType) {
        if (actionType != null && !actionType.isEmpty()) {
            MDC.put(ACTION_TYPE, actionType);
        }
    }
    
    /**
     * Set the HTTP status code in MDC.
     * 
     * @param httpStatus The HTTP status code
     */
    public static void setHttpStatus(int httpStatus) {
        MDC.put(HTTP_STATUS, String.valueOf(httpStatus));
    }
    
    /**
     * Clear a specific MDC key.
     * 
     * @param key The MDC key to clear
     */
    public static void clearMdcKey(String key) {
        MDC.remove(key);
    }
    
    /**
     * Clear the user ID from MDC.
     */
    public static void clearUserId() {
        MDC.remove(USER_ID);
    }
    
    /**
     * Clear the action type from MDC.
     */
    public static void clearActionType() {
        MDC.remove(ACTION_TYPE);
    }
    
    /**
     * Clear all MDC context.
     * Should be called at the end of request processing to prevent data leakage.
     */
    public static void clearMdc() {
        MDC.clear();
    }
    
    /**
     * Log with action type context.
     * Temporarily sets the action type, logs the message, then clears it.
     * 
     * @param logger The logger to use
     * @param actionType The action type for context
     * @param message The log message
     * @param args Message arguments
     */
    public static void logWithAction(Logger logger, String actionType, String message, Object... args) {
        try {
            setActionType(actionType);
            logger.info(message, args);
        } finally {
            clearActionType();
        }
    }
    
    /**
     * Mask sensitive data in log output.
     * Partially masks the input by showing only the first and last characters.
     * 
     * @param sensitiveData The data to mask
     * @return Masked version of the data
     */
    public static String maskSensitiveData(String sensitiveData) {
        if (sensitiveData == null || sensitiveData.isEmpty()) {
            return "****";
        }
        if (sensitiveData.length() <= 2) {
            return "****";
        }
        if (sensitiveData.length() <= 4) {
            return sensitiveData.charAt(0) + "**" + sensitiveData.charAt(sensitiveData.length() - 1);
        }
        return sensitiveData.charAt(0) + "***" + sensitiveData.charAt(sensitiveData.length() - 1);
    }
    
    /**
     * Mask email address for logging.
     * Shows first character of username and domain, masks the rest.
     * Example: john.doe@example.com -> j***e@e***.com
     * 
     * @param email The email to mask
     * @return Masked email
     */
    public static String maskEmail(String email) {
        if (email == null || email.isEmpty() || !email.contains("@")) {
            return "****";
        }
        String[] parts = email.split("@");
        if (parts.length != 2) {
            return "****";
        }
        String username = maskSensitiveData(parts[0]);
        String domain = maskSensitiveData(parts[1]);
        return username + "@" + domain;
    }
}
