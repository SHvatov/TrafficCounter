package com.ishvatov.spark;

import com.ishvatov.spark.model.entity.LimitsPerHourEntity;
import com.ishvatov.spark.service.TrafficService;
import com.ishvatov.spark.utils.Pair;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;

@EnableScheduling
@SpringBootApplication
@PropertySource(value = "classpath:schedule.properties")
@RequiredArgsConstructor
public class SparkStreamingApplication implements CommandLineRunner {
    private final ReadWriteLock lock;
    private final ScheduledExecutorService scheduler;
    private final TrafficService trafficService;

    @Value("${time.read.value}")
    private int readTimeValue;

    @Value("${time.read.units}")
    private TimeUnit readTimeUnits;

    @Value("${time.write.value}")
    private int writeTimeValue;

    @Value("${time.write.units}")
    private TimeUnit writeTimeUnits;

    // shared mutable data
    private Pair<LimitsPerHourEntity, LimitsPerHourEntity> limits = null;

    public static void main(String[] args) {
        SpringApplication.run(SparkStreamingApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        scheduler.scheduleAtFixedRate(
                this::fetchLimits, 0, writeTimeValue, writeTimeUnits
        );
        scheduler.scheduleAtFixedRate(
                this::validateTraffic, 0, readTimeValue, readTimeUnits
        );
    }

    /**
     * Synchronized block.
     */
    public void fetchLimits() {
        lock.writeLock().lock();
        try {
            limits = trafficService.fetchTrafficLimits();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Synchronized block.
     */
    public void validateTraffic() {
        // add PCap4J before locking

        lock.readLock().lock();
        try {
            trafficService.validateTransferredTraffic(0, limits);
        } finally {
            lock.readLock().unlock();
        }
    }
}
