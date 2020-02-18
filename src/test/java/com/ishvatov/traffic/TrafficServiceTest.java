package com.ishvatov.traffic;

import com.ishvatov.traffic.exception.InvalidLimitsNumberException;
import com.ishvatov.traffic.exception.InvalidLimitsValueException;
import com.ishvatov.traffic.model.entity.LimitsPerHourEntity;
import com.ishvatov.traffic.model.repository.LimitsPerHourRepository;
import com.ishvatov.traffic.service.TrafficService;
import com.ishvatov.traffic.utils.Pair;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Date;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * {@link com.ishvatov.traffic.service.TrafficServiceImpl} test class.
 *
 * @author ishvatov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(
        classes = TrafficCounterApplication.class,
        initializers = ConfigFileApplicationContextInitializer.class
)
public class TrafficServiceTest {
    private static final LimitsPerHourEntity CORRECT_MIN = new LimitsPerHourEntity(1, "min", 2048, new Date());
    private static final LimitsPerHourEntity CORRECT_MAX = new LimitsPerHourEntity(1, "max", 4096, new Date());
    private static final int DIFF_NOT_IN_RANGE = 100;
    private static final int DIFF_IN_RANGE = 3192;

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @MockBean
    private LimitsPerHourRepository repository;

    @Autowired
    private TrafficService trafficService;

    @Test
    public void Test_TrafficService_fetchTrafficLimits_CorrectLimits() {
        when(repository.findMinimumLimit()).thenReturn(Optional.ofNullable(CORRECT_MIN));
        when(repository.findMaximumLimit()).thenReturn(Optional.ofNullable(CORRECT_MAX));

        Pair<LimitsPerHourEntity, LimitsPerHourEntity> limits = trafficService.fetchTrafficLimits();
        assertEquals(new Pair<>(CORRECT_MIN, CORRECT_MAX), limits);
    }

    @Test(expected = InvalidLimitsValueException.class)
    public void Test_TrafficService_fetchTrafficLimits_MinBiggerThenMax() {
        when(repository.findMinimumLimit()).thenReturn(Optional.ofNullable(CORRECT_MAX));
        when(repository.findMaximumLimit()).thenReturn(Optional.ofNullable(CORRECT_MIN));

        Pair<LimitsPerHourEntity, LimitsPerHourEntity> limits = trafficService.fetchTrafficLimits();
    }

    @Test(expected = InvalidLimitsNumberException.class)
    public void Test_TrafficService_fetchTrafficLimits_MinIsNull() {
        when(repository.findMinimumLimit()).thenReturn(Optional.empty());
        when(repository.findMaximumLimit()).thenReturn(Optional.ofNullable(CORRECT_MAX));

        Pair<LimitsPerHourEntity, LimitsPerHourEntity> limits = trafficService.fetchTrafficLimits();
    }

    @Test(expected = InvalidLimitsNumberException.class)
    public void Test_TrafficService_fetchTrafficLimits_MaxIsNull() {
        when(repository.findMinimumLimit()).thenReturn(Optional.ofNullable(CORRECT_MIN));
        when(repository.findMaximumLimit()).thenReturn(Optional.empty());

        Pair<LimitsPerHourEntity, LimitsPerHourEntity> limits = trafficService.fetchTrafficLimits();
    }

    @Test
    public void Test_TrafficService_validateTransferredTraffic_TrafficNotInRange() {
        when(kafkaTemplate.send(anyString(), anyString())).thenReturn(null);

        boolean result = trafficService.validateTrafficAndSendNotification(
                DIFF_NOT_IN_RANGE,
                new Pair<>(CORRECT_MIN, CORRECT_MAX)
        );

        assertFalse(result);
    }

    @Test
    public void Test_TrafficService_validateTransferredTraffic_TrafficInRange() {
        when(kafkaTemplate.send(anyString(), anyString())).thenReturn(null);

        boolean result = trafficService.validateTrafficAndSendNotification(
                DIFF_IN_RANGE,
                new Pair<>(CORRECT_MIN, CORRECT_MAX)
        );

        assertTrue(result);
    }
}
