package com.empress.usermanagementapi.service;

import com.empress.usermanagementapi.dto.LogEntry;
import com.empress.usermanagementapi.dto.LogSummaryResponse;
import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for summarizing logs using AI-powered analysis.
 * Integrates with OpenAI API for intelligent log summarization.
 * Falls back to rule-based summarization if OpenAI is unavailable.
 */
@Service
public class LogSummarizerService {
    
    private static final Logger log = LoggerFactory.getLogger(LogSummarizerService.class);
    
    private final OpenAIClient openAIClient;
    
    @Autowired
    public LogSummarizerService(OpenAIClient openAIClient) {
        this.openAIClient = openAIClient;
    }
    
    /**
     * Summarize a list of sanitized log entries.
     * 
     * @param logEntries List of sanitized log entries
     * @param startTime Start time of the analysis period
     * @param endTime End time of the analysis period
     * @return Log summary response with AI-generated insights
     */
    public LogSummaryResponse summarizeLogs(List<LogEntry> logEntries, Instant startTime, Instant endTime) {
        log.info("Summarizing {} log entries from {} to {}", logEntries.size(), startTime, endTime);
        
        // Calculate statistics
        Map<String, Integer> logLevelStats = calculateLogLevelStats(logEntries);
        Map<String, Integer> actionTypeStats = calculateActionTypeStats(logEntries);
        List<String> topIssues = identifyTopIssues(logEntries);
        
        // Generate AI summary
        String aiSummary = generateAISummary(logEntries, startTime, endTime, logLevelStats, actionTypeStats, topIssues);
        
        return LogSummaryResponse.builder()
                .summary(aiSummary)
                .totalLogs(logEntries.size())
                .startTime(startTime != null ? startTime.toString() : null)
                .endTime(endTime != null ? endTime.toString() : null)
                .logLevelStats(logLevelStats)
                .actionTypeStats(actionTypeStats)
                .topIssues(topIssues)
                .build();
    }
    
    /**
     * Calculate statistics by log level.
     * 
     * @param logEntries List of log entries
     * @return Map of log level to count
     */
    private Map<String, Integer> calculateLogLevelStats(List<LogEntry> logEntries) {
        return logEntries.stream()
                .filter(entry -> entry.getLevel() != null)
                .collect(Collectors.groupingBy(
                        LogEntry::getLevel,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));
    }
    
    /**
     * Calculate statistics by action type.
     * 
     * @param logEntries List of log entries
     * @return Map of action type to count
     */
    private Map<String, Integer> calculateActionTypeStats(List<LogEntry> logEntries) {
        return logEntries.stream()
                .filter(entry -> entry.getActionType() != null)
                .collect(Collectors.groupingBy(
                        LogEntry::getActionType,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));
    }
    
    /**
     * Identify top issues from error and warning logs.
     * 
     * @param logEntries List of log entries
     * @return List of top issues
     */
    private List<String> identifyTopIssues(List<LogEntry> logEntries) {
        // Group error and warning messages by similarity
        Map<String, Integer> issueCount = new HashMap<>();
        
        logEntries.stream()
                .filter(entry -> "ERROR".equals(entry.getLevel()) || "WARN".equals(entry.getLevel()))
                .forEach(entry -> {
                    String message = entry.getMessage();
                    if (message != null && !message.isEmpty()) {
                        // Extract issue type from message (simplified approach)
                        String issueType = extractIssueType(message);
                        issueCount.merge(issueType, 1, Integer::sum);
                    }
                });
        
        // Return top 5 issues sorted by frequency
        return issueCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .map(entry -> entry.getKey() + " (occurred " + entry.getValue() + " times)")
                .collect(Collectors.toList());
    }
    
    /**
     * Extract issue type from error/warning message.
     * This is a simplified approach - can be enhanced with NLP.
     * 
     * @param message Error or warning message
     * @return Issue type string
     */
    private String extractIssueType(String message) {
        // Look for common patterns in error messages
        if (message.contains("failed") || message.contains("failure")) {
            if (message.contains("login")) return "Login failure";
            if (message.contains("registration")) return "Registration failure";
            if (message.contains("validation")) return "Validation failure";
            if (message.contains("user")) return "User operation failure";
            if (message.contains("database") || message.contains("connection")) return "Database connection issue";
            return "Operation failure";
        }
        
        if (message.contains("not found")) {
            return "Resource not found";
        }
        
        if (message.contains("already exists") || message.contains("duplicate")) {
            return "Duplicate resource";
        }
        
        if (message.contains("unauthorized") || message.contains("forbidden")) {
            return "Authorization issue";
        }
        
        if (message.contains("timeout")) {
            return "Timeout issue";
        }
        
        // Default: return first 50 characters of the message
        return message.length() > 50 ? message.substring(0, 50) + "..." : message;
    }
    
    /**
     * Generate AI-powered summary of the logs.
     * Uses OpenAI API if available, otherwise falls back to rule-based summarization.
     * 
     * @param logEntries List of log entries
     * @param startTime Start time of analysis
     * @param endTime End time of analysis
     * @param logLevelStats Log level statistics
     * @param actionTypeStats Action type statistics
     * @param topIssues List of top issues
     * @return AI-generated summary
     */
    private String generateAISummary(List<LogEntry> logEntries, Instant startTime, Instant endTime,
                                     Map<String, Integer> logLevelStats,
                                     Map<String, Integer> actionTypeStats,
                                     List<String> topIssues) {
        
        // Try OpenAI API if client is available
        if (openAIClient != null) {
            try {
                String openAISummary = generateOpenAISummary(logEntries, startTime, endTime, 
                                                             logLevelStats, actionTypeStats, topIssues);
                if (openAISummary != null && !openAISummary.isEmpty()) {
                    log.info("Successfully generated OpenAI-powered log summary");
                    return openAISummary;
                }
            } catch (Exception e) {
                log.warn("Failed to generate OpenAI summary, falling back to rule-based approach: {}", e.getMessage());
            }
        } else {
            log.debug("OpenAI client not configured, using rule-based summarization");
        }
        
        // Fall back to rule-based summary
        return generateRuleBasedSummary(logEntries, startTime, endTime, logLevelStats, actionTypeStats, topIssues);
    }
    
    /**
     * Generate summary using OpenAI API.
     * 
     * @param logEntries List of log entries
     * @param startTime Start time of analysis
     * @param endTime End time of analysis
     * @param logLevelStats Log level statistics
     * @param actionTypeStats Action type statistics
     * @param topIssues List of top issues
     * @return OpenAI-generated summary or null if failed
     */
    private String generateOpenAISummary(List<LogEntry> logEntries, Instant startTime, Instant endTime,
                                        Map<String, Integer> logLevelStats,
                                        Map<String, Integer> actionTypeStats,
                                        List<String> topIssues) {
        
        String timePeriod = calculateTimePeriod(startTime, endTime);
        
        // Build prompt with log statistics
        StringBuilder prompt = new StringBuilder();
        prompt.append("Analyze the following log data and provide a concise, professional summary:\n\n");
        prompt.append("Time Period: ").append(timePeriod).append("\n");
        prompt.append("Total Events: ").append(logEntries.size()).append("\n\n");
        
        if (!logLevelStats.isEmpty()) {
            prompt.append("Log Levels:\n");
            logLevelStats.forEach((level, count) -> 
                prompt.append("- ").append(level).append(": ").append(count).append("\n"));
            prompt.append("\n");
        }
        
        if (!actionTypeStats.isEmpty()) {
            prompt.append("Action Types:\n");
            actionTypeStats.forEach((action, count) -> 
                prompt.append("- ").append(action).append(": ").append(count).append("\n"));
            prompt.append("\n");
        }
        
        if (!topIssues.isEmpty()) {
            prompt.append("Top Issues:\n");
            topIssues.forEach(issue -> prompt.append("- ").append(issue).append("\n"));
            prompt.append("\n");
        }
        
        // Add sample log entries for context (limit to 10 most recent)
        prompt.append("Sample Log Entries:\n");
        logEntries.stream()
                .limit(10)
                .forEach(entry -> {
                    prompt.append("- [").append(entry.getLevel()).append("] ");
                    if (entry.getActionType() != null) {
                        prompt.append(entry.getActionType()).append(": ");
                    }
                    prompt.append(entry.getMessage()).append("\n");
                });
        
        prompt.append("\nProvide a summary that includes:\n");
        prompt.append("1. Overall system health assessment\n");
        prompt.append("2. Key patterns or trends\n");
        prompt.append("3. Critical issues requiring attention\n");
        prompt.append("4. Actionable recommendations\n");
        
        log.debug("Calling OpenAI API with prompt length: {} characters", prompt.length());
        
        try {
            ChatCompletionMessageParam userMessage = ChatCompletionMessageParam.ofUser(
                    ChatCompletionUserMessageParam.builder()
                            .content(ChatCompletionUserMessageParam.Content.ofText(prompt.toString()))
                            .build()
            );
            
            ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                    .model("gpt-3.5-turbo")
                    .addMessage(userMessage)
                    .maxTokens(500)
                    .temperature(0.7)
                    .build();
            
            ChatCompletion completion = openAIClient.chat().completions().create(params);
            
            String summary = completion.choices().get(0).message().content().get();
            log.debug("Received OpenAI summary with length: {} characters", summary.length());
            return summary;
            
        } catch (Exception e) {
            log.error("Error calling OpenAI API: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Generate rule-based summary (original implementation).
     * 
     * @param logEntries List of log entries
     * @param startTime Start time of analysis
     * @param endTime End time of analysis
     * @param logLevelStats Log level statistics
     * @param actionTypeStats Action type statistics
     * @param topIssues List of top issues
     * @return Rule-based summary
     */
    private String generateRuleBasedSummary(List<LogEntry> logEntries, Instant startTime, Instant endTime,
                                           Map<String, Integer> logLevelStats,
                                           Map<String, Integer> actionTypeStats,
                                           List<String> topIssues) {
        
        // Calculate time period
        String timePeriod = calculateTimePeriod(startTime, endTime);
        
        // Build summary using statistics
        StringBuilder summary = new StringBuilder();
        summary.append("Log Analysis Summary for ").append(timePeriod).append(":\n\n");
        
        // Overall activity
        summary.append("Total Events: ").append(logEntries.size()).append("\n");
        
        // Log level breakdown
        if (!logLevelStats.isEmpty()) {
            summary.append("\nLog Level Distribution:\n");
            int errorCount = logLevelStats.getOrDefault("ERROR", 0);
            int warnCount = logLevelStats.getOrDefault("WARN", 0);
            int infoCount = logLevelStats.getOrDefault("INFO", 0);
            
            if (errorCount > 0) {
                summary.append("- ").append(errorCount).append(" errors detected\n");
            }
            if (warnCount > 0) {
                summary.append("- ").append(warnCount).append(" warnings\n");
            }
            if (infoCount > 0) {
                summary.append("- ").append(infoCount).append(" informational events\n");
            }
        }
        
        // Action type insights
        if (!actionTypeStats.isEmpty()) {
            summary.append("\nUser Activity:\n");
            int loginCount = actionTypeStats.getOrDefault("USER_LOGIN", 0);
            int registrationCount = actionTypeStats.getOrDefault("USER_REGISTRATION", 0);
            int createCount = actionTypeStats.getOrDefault("USER_CREATE", 0);
            int updateCount = actionTypeStats.getOrDefault("USER_UPDATE", 0);
            int deleteCount = actionTypeStats.getOrDefault("USER_DELETE", 0);
            
            if (loginCount > 0) {
                summary.append("- ").append(loginCount).append(" login attempts\n");
            }
            if (registrationCount > 0) {
                summary.append("- ").append(registrationCount).append(" registration attempts\n");
            }
            if (createCount > 0) {
                summary.append("- ").append(createCount).append(" users created\n");
            }
            if (updateCount > 0) {
                summary.append("- ").append(updateCount).append(" user updates\n");
            }
            if (deleteCount > 0) {
                summary.append("- ").append(deleteCount).append(" user deletions\n");
            }
        }
        
        // Top issues
        if (!topIssues.isEmpty()) {
            summary.append("\nTop Issues:\n");
            topIssues.forEach(issue -> summary.append("- ").append(issue).append("\n"));
        }
        
        // Generate insights
        summary.append("\nInsights:\n");
        generateInsights(summary, logLevelStats, actionTypeStats);
        
        log.debug("Generated AI summary with {} characters", summary.length());
        return summary.toString();
    }
    
    /**
     * Calculate human-readable time period description.
     * 
     * @param startTime Start time
     * @param endTime End time
     * @return Human-readable time period
     */
    private String calculateTimePeriod(Instant startTime, Instant endTime) {
        if (startTime == null && endTime == null) {
            return "all available logs";
        }
        
        if (startTime != null && endTime != null) {
            long hours = ChronoUnit.HOURS.between(startTime, endTime);
            long days = ChronoUnit.DAYS.between(startTime, endTime);
            
            if (hours < 1) {
                long minutes = ChronoUnit.MINUTES.between(startTime, endTime);
                return "the last " + minutes + " minutes";
            } else if (hours < 24) {
                return "the last " + hours + " hours";
            } else {
                return "the last " + days + " days";
            }
        }
        
        return "the specified time period";
    }
    
    /**
     * Generate insights from statistics.
     * 
     * @param summary StringBuilder to append insights to
     * @param logLevelStats Log level statistics
     * @param actionTypeStats Action type statistics
     */
    private void generateInsights(StringBuilder summary, Map<String, Integer> logLevelStats,
                                  Map<String, Integer> actionTypeStats) {
        
        int totalLogs = logLevelStats.values().stream().mapToInt(Integer::intValue).sum();
        int errorCount = logLevelStats.getOrDefault("ERROR", 0);
        int warnCount = logLevelStats.getOrDefault("WARN", 0);
        
        // Error rate analysis
        if (totalLogs > 0) {
            double errorRate = (errorCount * 100.0) / totalLogs;
            if (errorRate > 5) {
                summary.append("- High error rate detected (").append(String.format("%.1f", errorRate)).append("%)\n");
            } else if (errorRate > 0) {
                summary.append("- Low error rate (").append(String.format("%.1f", errorRate)).append("%)\n");
            } else {
                summary.append("- No errors detected in this period\n");
            }
        }
        
        // Login failure analysis
        int loginAttempts = actionTypeStats.getOrDefault("USER_LOGIN", 0);
        if (loginAttempts > 10 && errorCount > 0) {
            summary.append("- Multiple login attempts detected, review for potential security concerns\n");
        }
        
        // Registration analysis
        int registrations = actionTypeStats.getOrDefault("USER_REGISTRATION", 0);
        int userCreations = actionTypeStats.getOrDefault("USER_CREATE", 0);
        if (registrations > 0 || userCreations > 0) {
            summary.append("- User growth: ").append(registrations + userCreations).append(" new users\n");
        }
    }
}
