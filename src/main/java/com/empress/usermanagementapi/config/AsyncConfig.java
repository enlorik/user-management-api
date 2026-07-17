package com.empress.usermanagementapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Asynchronous task execution configuration.
 *
 * Provides a bounded executor for account recovery processing so that
 * account-dependent work (lookup, token creation, email delivery) runs off
 * the HTTP request thread and the response time no longer reveals whether
 * an account exists.
 *
 * The rejection policy is deliberately AbortPolicy rather than
 * CallerRunsPolicy: running the work on the caller thread would reintroduce
 * the timing difference this executor exists to remove.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "accountRecoveryExecutor")
    public ThreadPoolTaskExecutor accountRecoveryExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("account-recovery-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }
}
