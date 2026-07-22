package com.empress.usermanagementapi.config;

import com.empress.usermanagementapi.service.AccountRecoveryService;
import com.empress.usermanagementapi.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Wiring test for the asynchronous account recovery executor.
 *
 * Proves that the accountRecoveryExecutor bean exists with the expected
 * bounded configuration and that AccountRecoveryService.processResetRequest
 * actually executes on an executor thread rather than the caller thread.
 */
@SpringBootTest
class AsyncConfigTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private AccountRecoveryService accountRecoveryService;

    @MockBean
    private UserService userService;

    @Test
    void accountRecoveryExecutorBeanExistsWithBoundedConfiguration() {
        ThreadPoolTaskExecutor executor =
                context.getBean("accountRecoveryExecutor", ThreadPoolTaskExecutor.class);

        assertNotNull(executor);
        assertEquals(2, executor.getCorePoolSize());
        assertEquals(4, executor.getMaxPoolSize());
        assertEquals(100, executor.getQueueCapacity());
        assertEquals("account-recovery-", executor.getThreadNamePrefix());
    }

    @Test
    void processResetRequest_RunsOnExecutorThreadNotCallerThread() throws Exception {
        AtomicReference<String> executingThread = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        when(userService.findByUsernameAndEmail(anyString(), anyString()))
                .thenAnswer(invocation -> {
                    executingThread.set(Thread.currentThread().getName());
                    latch.countDown();
                    return Optional.empty();
                });

        accountRecoveryService.processResetRequest("someone", "someone@example.com");

        assertTrue(latch.await(5, TimeUnit.SECONDS),
                "asynchronous account recovery task did not run");
        assertNotEquals(Thread.currentThread().getName(), executingThread.get(),
                "account recovery must not run on the caller thread");
        assertTrue(executingThread.get().startsWith("account-recovery-"),
                "expected the dedicated executor thread, got: " + executingThread.get());
    }
}
