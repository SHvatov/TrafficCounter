package com.ishvatov.spark.service;

import com.ishvatov.spark.model.entity.LimitsPerHourEntity;
import com.ishvatov.spark.model.repository.LimitsPerHourRepository;
import com.ishvatov.spark.utils.Pair;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@PropertySource(value = "classpath:kafka.properties")
public class TrafficServiceImpl implements TrafficService {
    private final LimitsPerHourRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value(value = "${kafka.topic-name}")
    private String topicName;

    @Value(value = "${kafka.alert-message}")
    private String alertMessage;

    @Override
    public Pair<LimitsPerHourEntity, LimitsPerHourEntity> fetchTrafficLimits() {
        return repository.findLimits();
    }

    @Override
    public void validateTransferredTraffic(int current, Pair<LimitsPerHourEntity, LimitsPerHourEntity> limits) {
        if (current < limits.getFirst().getLimitValue() || current > limits.getSecond().getLimitValue()) {
            kafkaTemplate.send(
                    topicName,
                    String.format(alertMessage, current, limits.getFirst(), limits.getSecond())
            );
        }
    }
}
