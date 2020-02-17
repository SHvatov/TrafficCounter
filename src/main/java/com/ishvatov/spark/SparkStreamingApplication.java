package com.ishvatov.spark;

import com.ishvatov.spark.exception.PcapRuntimeException;
import com.ishvatov.spark.model.entity.LimitsPerHourEntity;
import com.ishvatov.spark.service.TrafficService;
import com.ishvatov.spark.utils.Pair;
import lombok.RequiredArgsConstructor;
import org.pcap4j.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.annotation.PreDestroy;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;

@EnableScheduling
@SpringBootApplication
@RequiredArgsConstructor
@PropertySource(value = "classpath:schedule.properties")
public class SparkStreamingApplication implements CommandLineRunner {
    // logger
    private static final Logger LOGGER = LoggerFactory.getLogger(SparkStreamingApplication.class);

    // pcap handler initialization constants
    private static final int INFINITE_PACKET_NUMBER = -1;
    private static final String DEFAULT_DEV = "any";
    private static final int SNAP_LEN = 65536;
    private static final int TIMEOUT = 10;

    // autowired dependencies
    private final ReadWriteLock lock;
    private final TrafficService trafficService;
    private final ScheduledExecutorService scheduler;

    @Value("${time.read.value}")
    private int readTimeValue;

    @Value("${time.read.units}")
    private TimeUnit readTimeUnits;

    @Value("${time.write.value}")
    private int writeTimeValue;

    @Value("${time.write.units}")
    private TimeUnit writeTimeUnits;

    // Shared mutable variable, which stores the information about limits
    private Pair<LimitsPerHourEntity, LimitsPerHourEntity> limits = new Pair<>(null, null);

    // Pcap handler, which is used to interact with pcap api
    private PcapHandle pcapHandler = null;

    // stores the amount of the traffic that has already been transferred.
    private int totalTransferredData = 0;

    // stores the amount of the traffic that has been transferred previously.
    private int previousTransferredData = 0;

    public static void main(String[] args) {
        SpringApplication.run(SparkStreamingApplication.class, args);
    }

    @PreDestroy
    public void freeResources() throws InterruptedException {
        // stop the executor service
        scheduler.shutdown();
        if (!scheduler.awaitTermination(1, TimeUnit.MINUTES)) {
            scheduler.shutdownNow();
        }

        // close the Pcap handler
        pcapHandler.close();

        // log about shutdown
        LOGGER.info("Application is shutdown!");
    }

    @Override
    public void run(String... args) {
        try {
            // initialize the pcap interface
            PcapNetworkInterface pcapNetworkInterface = Pcaps.getDevByName(DEFAULT_DEV);

            // init filter if needed
            String filter = null;
            if (args.length != 0) {
                filter = String.format("net %s", args[0]);
            }

            LOGGER.info(
                    "Using the following device: {} with the follwoing filter: {}",
                    pcapNetworkInterface,
                    filter == null ? "None" : filter
            );

            // open pcap handle which is used to set listeners and get the results
            pcapHandler = pcapNetworkInterface.openLive(
                    SNAP_LEN,
                    PcapNetworkInterface.PromiscuousMode.PROMISCUOUS,
                    TIMEOUT
            );

            // set filter
            if (filter != null) {
                pcapHandler.setFilter(filter, BpfProgram.BpfCompileMode.OPTIMIZE);
            }

            // start pcap loop
            scheduler.execute(
                    () -> {
                        try {
                            // add listener on the handler
                            pcapHandler.loop(
                                    INFINITE_PACKET_NUMBER,
                                    (PcapPacket packet) -> {
                                        totalTransferredData += packet.length();
                                        LOGGER.info(
                                                String.format(
                                                        "Received packet!\n\tCurrent data amount: %s\n\tPrevious: %s",
                                                        totalTransferredData,
                                                        previousTransferredData
                                                )
                                        );
                                    }
                            );
                        } catch (Exception exception) {
                            throw new PcapRuntimeException(exception);
                        }
                    }
            );

            // schedule and start fetching limits process
            scheduler.scheduleAtFixedRate(
                    this::fetchLimits, 0, writeTimeValue, writeTimeUnits
            );

            // schedule  traffic validation process
            scheduler.scheduleAtFixedRate(
                    this::validateTraffic, readTimeValue, readTimeValue, readTimeUnits
            );
        } catch (Exception ex) {
            LOGGER.error(String.format("Following error has occurred: %s", ex.getMessage()), ex);
        }
    }

    /**
     * Synchronized block.
     */
    public void fetchLimits() {
        lock.writeLock().lock();
        try {
            limits.update(trafficService.fetchTrafficLimits());
            LOGGER.info(String.format("Fetched limits from database: %s!", limits));
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Synchronized block.
     */
    public void validateTraffic() {
        lock.readLock().lock();
        try {
            // make thread local copy of the variable to prevent
            // data loose due to the variable value update in the 'loop' thread
            int tempTotal = totalTransferredData;
            int difference = tempTotal - previousTransferredData;
            previousTransferredData = tempTotal;

            if (trafficService.validateTrafficAndSendNotification(difference, limits)) {
                LOGGER.info("Current traffic [{}] is out of range! Notification was sent!", difference);
            } else {
                LOGGER.info("Current traffic is in range!");
            }
        } finally {
            lock.readLock().unlock();
        }
    }
}
