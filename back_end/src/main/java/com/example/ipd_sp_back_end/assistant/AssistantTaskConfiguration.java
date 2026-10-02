package com.example.ipd_sp_back_end.assistant;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableScheduling
public class AssistantTaskConfiguration {
    @Bean("assistantTaskExecutor")
    public ThreadPoolTaskExecutor assistantTaskExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(8);
        executor.setThreadNamePrefix("assistant-generation-");
        executor.setWaitForTasksToCompleteOnShutdown(false);
        return executor;
    }
}
