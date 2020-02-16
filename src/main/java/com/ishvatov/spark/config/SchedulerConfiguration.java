package com.ishvatov.spark.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Configuration
public class SchedulerConfiguration {
    @Bean
    public ScheduledExecutorService scheduledExecutorService() {
        return Executors.newScheduledThreadPool(3);
    }

    @Bean
    public ReadWriteLock readWriteLock() {
        return new ReentrantReadWriteLock();
    }
}
