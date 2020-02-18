package com.ishvatov.traffic.service;

import com.ishvatov.traffic.exception.InvalidLimitsNumberException;
import com.ishvatov.traffic.exception.InvalidLimitsValueException;
import com.ishvatov.traffic.model.entity.LimitsPerHourEntity;
import com.ishvatov.traffic.model.repository.LimitsPerHourRepository;
import com.ishvatov.traffic.utils.Pair;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@PropertySource(value = "classpath:kafka.properties")
public class TrafficServiceImpl implements TrafficService {
    // autowired variables
    private final LimitsPerHourRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value(value = "${kafka.topic-name}")
    private String topicName;

    @Value(value = "${kafka.alert-message}")
    private String alertMessage;

    @Override
    public Pair<LimitsPerHourEntity, LimitsPerHourEntity> fetchTrafficLimits() {
        LimitsPerHourEntity min = repository.findMinimumLimit()
                .orElseThrow(InvalidLimitsNumberException::new);
        LimitsPerHourEntity max = repository.findMaximumLimit()
                .orElseThrow(InvalidLimitsNumberException::new);

        if (min.getLimitValue() > max.getLimitValue()) {
            throw new InvalidLimitsValueException();
        }
        return new Pair<>(min, max);
    }

    @Override
    public boolean validateTrafficAndSendNotification(int current, Pair<LimitsPerHourEntity, LimitsPerHourEntity> limits) {
        if (current < limits.getFirst().getLimitValue() || current > limits.getSecond().getLimitValue()) {
            kafkaTemplate.send(
                    topicName,
                    String.format(alertMessage, current, limits.getFirst(), limits.getSecond())
            );
            return true;
        }
        return false;
    }
}
