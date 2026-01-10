package com.empress.usermanagementapi.service;

import com.empress.usermanagementapi.dto.LogEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LogSanitizerService.
 */
@ExtendWith(MockitoExtension.class)
class LogSanitizerServiceTest {

    @InjectMocks
    private LogSanitizerService logSanitizerService;

    @Test
    void testSanitizeLogs_MasksEmailAddresses() {
        // Arrange
        LogEntry entry = new LogEntry();
        entry.setMessage("User registered with email john.doe@example.com");
        List<LogEntry> entries = List.of(entry);

        // Act
        List<LogEntry> sanitized = logSanitizerService.sanitizeLogs(entries);

        // Assert
        assertNotNull(sanitized);
        assertEquals(1, sanitized.size());
        assertFalse(sanitized.get(0).getMessage().contains("john.doe@example.com"));
        assertTrue(sanitized.get(0).getMessage().contains("@"));
    }

    @Test
    void testSanitizeLogs_MasksPhoneNumbers() {
        // Arrange
        LogEntry entry = new LogEntry();
        entry.setMessage("Contact phone: 555-123-4567");
        List<LogEntry> entries = List.of(entry);

        // Act
        List<LogEntry> sanitized = logSanitizerService.sanitizeLogs(entries);

        // Assert
        assertNotNull(sanitized);
        assertEquals(1, sanitized.size());
        assertFalse(sanitized.get(0).getMessage().contains("555-123-4567"));
        assertTrue(sanitized.get(0).getMessage().contains("***-***-****"));
    }

    @Test
    void testSanitizeLogs_MasksTokens() {
        // Arrange
        // Use a longer hex token (36 chars) to ensure it matches the pattern
        LogEntry entry = new LogEntry();
        entry.setMessage("Auth token: a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6");
        List<LogEntry> entries = List.of(entry);

        // Act
        List<LogEntry> sanitized = logSanitizerService.sanitizeLogs(entries);

        // Assert
        assertNotNull(sanitized);
        assertEquals(1, sanitized.size());
        // The original token should not be present
        assertFalse(sanitized.get(0).getMessage().contains("a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6"),
                "Original token should be removed");
    }

    @Test
    void testSanitizeLogs_MasksSSN() {
        // Arrange
        LogEntry entry = new LogEntry();
        entry.setMessage("SSN: 123-45-6789");
        List<LogEntry> entries = List.of(entry);

        // Act
        List<LogEntry> sanitized = logSanitizerService.sanitizeLogs(entries);

        // Assert
        assertNotNull(sanitized);
        assertEquals(1, sanitized.size());
        assertFalse(sanitized.get(0).getMessage().contains("123-45-6789"));
        assertTrue(sanitized.get(0).getMessage().contains("***-**-****"));
    }

    @Test
    void testSanitizeLogs_PreservesNonSensitiveData() {
        // Arrange
        LogEntry entry = new LogEntry();
        entry.setLevel("INFO");
        entry.setLogger("com.empress.test");
        entry.setActionType("USER_LOGIN");
        entry.setMessage("User logged in successfully");
        List<LogEntry> entries = List.of(entry);

        // Act
        List<LogEntry> sanitized = logSanitizerService.sanitizeLogs(entries);

        // Assert
        assertNotNull(sanitized);
        assertEquals(1, sanitized.size());
        assertEquals("INFO", sanitized.get(0).getLevel());
        assertEquals("com.empress.test", sanitized.get(0).getLogger());
        assertEquals("USER_LOGIN", sanitized.get(0).getActionType());
        assertEquals("User logged in successfully", sanitized.get(0).getMessage());
    }

    @Test
    void testSanitizeLogs_HandlesNullMessage() {
        // Arrange
        LogEntry entry = new LogEntry();
        entry.setMessage(null);
        List<LogEntry> entries = List.of(entry);

        // Act
        List<LogEntry> sanitized = logSanitizerService.sanitizeLogs(entries);

        // Assert
        assertNotNull(sanitized);
        assertEquals(1, sanitized.size());
        assertNull(sanitized.get(0).getMessage());
    }

    @Test
    void testSanitizeLogs_HandlesEmptyList() {
        // Arrange
        List<LogEntry> entries = new ArrayList<>();

        // Act
        List<LogEntry> sanitized = logSanitizerService.sanitizeLogs(entries);

        // Assert
        assertNotNull(sanitized);
        assertTrue(sanitized.isEmpty());
    }

    @Test
    void testSanitizeLogs_HandlesMultipleEntries() {
        // Arrange
        LogEntry entry1 = new LogEntry();
        entry1.setMessage("Email: test@example.com");
        
        LogEntry entry2 = new LogEntry();
        entry2.setMessage("Phone: 555-123-4567");
        
        LogEntry entry3 = new LogEntry();
        entry3.setMessage("Normal log message");
        
        List<LogEntry> entries = List.of(entry1, entry2, entry3);

        // Act
        List<LogEntry> sanitized = logSanitizerService.sanitizeLogs(entries);

        // Assert
        assertNotNull(sanitized);
        assertEquals(3, sanitized.size());
        assertFalse(sanitized.get(0).getMessage().contains("test@example.com"));
        assertFalse(sanitized.get(1).getMessage().contains("555-123-4567"));
        assertEquals("Normal log message", sanitized.get(2).getMessage());
    }
}
