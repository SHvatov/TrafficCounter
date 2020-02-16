package com.ishvatov.spark;

import com.ishvatov.spark.exception.PcapRuntimeException;
import com.ishvatov.spark.model.entity.LimitsPerHourEntity;
import com.ishvatov.spark.service.TrafficService;
import com.ishvatov.spark.utils.Pair;
import lombok.RequiredArgsConstructor;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.PcapPacket;
import org.pcap4j.core.Pcaps;
import org.pcap4j.util.NifSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.annotation.PreDestroy;
import java.net.InetAddress;
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
    private Pair<LimitsPerHourEntity, LimitsPerHourEntity> limits = null;

    // Pcap handler, which is used to interact with pcap api
    private PcapHandle pcapHandle = null;

    // stores the amount of the traffic that has already been transferred.
    private int currentTransferredData = 0; // in bytes

    // stores the amount of the traffic that has been transferred previously.
    private int previousTransferredData = 0; // in bytes

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
        pcapHandle.close();

        // log about shutdown
        LOGGER.info("Application is shutdown!");
    }

    @Override
    public void run(String... args) {
        try {
            // initialize the pcap interface
            PcapNetworkInterface pcapNetworkInterface;
            if (args.length != 0) {
                InetAddress addr = InetAddress.getByName(args[0]);
                pcapNetworkInterface = Pcaps.getDevByAddress(addr);
            } else {
                pcapNetworkInterface = new NifSelector().selectNetworkInterface();
            }

            // open pcap handle which is used to set listeners and get the results
            pcapHandle = pcapNetworkInterface.openLive(
                    SNAP_LEN,
                    PcapNetworkInterface.PromiscuousMode.PROMISCUOUS,
                    TIMEOUT
            );

            // start pcap loop
            scheduler.execute(
                    () -> {
                        try {
                            // add listener on the handler
                            pcapHandle.loop(
                                    INFINITE_PACKET_NUMBER,
                                    (PcapPacket packet) -> {
                                        currentTransferredData += packet.getRawData().length;
                                        LOGGER.info(
                                                String.format(
                                                        "Received packet! Current data amount: %s",
                                                        currentTransferredData
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
            limits = trafficService.fetchTrafficLimits();
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
            int tempCurrent = currentTransferredData;
            int difference = tempCurrent - previousTransferredData;
            previousTransferredData = tempCurrent;

            trafficService.validateTransferredTraffic(difference, limits);
            LOGGER.info("Validated limits from database!");
        } finally {
            lock.readLock().unlock();
        }
    }
}
