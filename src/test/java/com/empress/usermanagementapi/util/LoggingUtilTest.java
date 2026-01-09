package com.empress.usermanagementapi.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for LoggingUtil.
 * Verifies metadata management and sensitive data masking functionality.
 */
class LoggingUtilTest {

    @AfterEach
    void cleanup() {
        // Clean up MDC after each test
        LoggingUtil.clearMdc();
    }

    @Test
    void testGenerateRequestId() {
        String requestId = LoggingUtil.generateRequestId();
        
        assertNotNull(requestId);
        assertFalse(requestId.isEmpty());
        assertEquals(requestId, MDC.get(LoggingUtil.REQUEST_ID));
    }

    @Test
    void testSetRequestId() {
        String testRequestId = "test-request-123";
        LoggingUtil.setRequestId(testRequestId);
        
        assertEquals(testRequestId, MDC.get(LoggingUtil.REQUEST_ID));
    }

    @Test
    void testSetUserId() {
        Long userId = 42L;
        LoggingUtil.setUserId(userId);
        
        assertEquals("42", MDC.get(LoggingUtil.USER_ID));
    }

    @Test
    void testSetUserIdString() {
        String userId = "user123";
        LoggingUtil.setUserId(userId);
        
        assertEquals(userId, MDC.get(LoggingUtil.USER_ID));
    }

    @Test
    void testSetActionType() {
        String actionType = "USER_LOGIN";
        LoggingUtil.setActionType(actionType);
        
        assertEquals(actionType, MDC.get(LoggingUtil.ACTION_TYPE));
    }

    @Test
    void testSetHttpStatus() {
        LoggingUtil.setHttpStatus(200);
        
        assertEquals("200", MDC.get(LoggingUtil.HTTP_STATUS));
    }

    @Test
    void testClearMdc() {
        LoggingUtil.setRequestId("test-request");
        LoggingUtil.setUserId(123L);
        LoggingUtil.setActionType("TEST_ACTION");
        
        LoggingUtil.clearMdc();
        
        assertNull(MDC.get(LoggingUtil.REQUEST_ID));
        assertNull(MDC.get(LoggingUtil.USER_ID));
        assertNull(MDC.get(LoggingUtil.ACTION_TYPE));
    }

    @Test
    void testClearUserId() {
        LoggingUtil.setUserId(123L);
        assertEquals("123", MDC.get(LoggingUtil.USER_ID));
        
        LoggingUtil.clearUserId();
        assertNull(MDC.get(LoggingUtil.USER_ID));
    }

    @Test
    void testClearActionType() {
        LoggingUtil.setActionType("TEST_ACTION");
        assertEquals("TEST_ACTION", MDC.get(LoggingUtil.ACTION_TYPE));
        
        LoggingUtil.clearActionType();
        assertNull(MDC.get(LoggingUtil.ACTION_TYPE));
    }

    @Test
    void testMaskSensitiveData() {
        assertEquals("****", LoggingUtil.maskSensitiveData(null));
        assertEquals("****", LoggingUtil.maskSensitiveData(""));
        assertEquals("****", LoggingUtil.maskSensitiveData("a"));
        assertEquals("****", LoggingUtil.maskSensitiveData("ab"));
        assertEquals("a**c", LoggingUtil.maskSensitiveData("abc"));
        assertEquals("a**d", LoggingUtil.maskSensitiveData("abcd"));
        assertEquals("p***d", LoggingUtil.maskSensitiveData("password"));
    }

    @Test
    void testMaskEmail() {
        assertEquals("****", LoggingUtil.maskEmail(null));
        assertEquals("****", LoggingUtil.maskEmail(""));
        assertEquals("****", LoggingUtil.maskEmail("notanemail"));
        assertEquals("****", LoggingUtil.maskEmail("@"));
        assertEquals("j**n@e***e", LoggingUtil.maskEmail("john@example"));
        assertEquals("j***e@e***m", LoggingUtil.maskEmail("john.doe@example.com"));
        assertEquals("a**c@t**t", LoggingUtil.maskEmail("abc@test"));
    }

    @Test
    void testSetNullValues() {
        // Should not throw exceptions or set MDC values
        LoggingUtil.setRequestId(null);
        assertNull(MDC.get(LoggingUtil.REQUEST_ID));
        
        LoggingUtil.setUserId((String) null);
        assertNull(MDC.get(LoggingUtil.USER_ID));
        
        LoggingUtil.setUserId((Long) null);
        assertNull(MDC.get(LoggingUtil.USER_ID));
        
        LoggingUtil.setActionType(null);
        assertNull(MDC.get(LoggingUtil.ACTION_TYPE));
    }

    @Test
    void testSetEmptyValues() {
        // Should not set MDC values for empty strings
        LoggingUtil.setRequestId("");
        assertNull(MDC.get(LoggingUtil.REQUEST_ID));
        
        LoggingUtil.setUserId("");
        assertNull(MDC.get(LoggingUtil.USER_ID));
        
        LoggingUtil.setActionType("");
        assertNull(MDC.get(LoggingUtil.ACTION_TYPE));
    }
}
