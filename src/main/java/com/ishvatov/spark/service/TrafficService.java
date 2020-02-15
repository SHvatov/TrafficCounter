package com.ishvatov.spark.service;

import com.ishvatov.spark.exception.InvalidLimitsNumberException;
import com.ishvatov.spark.model.entity.LimitsPerHourEntity;
import com.ishvatov.spark.utils.Pair;

public interface TrafficService {

    /**
     * @return the pair of records from the limits_per_hour table with maximum effective_date value.
     * @throws InvalidLimitsNumberException if there is zero or only one record
     */
    Pair<LimitsPerHourEntity, LimitsPerHourEntity> fetchTrafficLimits();

    /**
     * Checks the current amount of transferred data nad if current < limit,
     * then sends specified alert message to specified kafka alert topic.
     *
     * @param current current amount of transferred data
     * @param limits  current limits of transferred data
     */
    void validateTransferredTraffic(int current, Pair<LimitsPerHourEntity, LimitsPerHourEntity> limits);
}