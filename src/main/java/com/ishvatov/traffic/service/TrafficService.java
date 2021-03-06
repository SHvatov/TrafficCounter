package com.ishvatov.traffic.service;

import com.ishvatov.traffic.exception.InvalidLimitsNumberException;
import com.ishvatov.traffic.exception.InvalidLimitsValueException;
import com.ishvatov.traffic.model.entity.LimitsPerHourEntity;
import com.ishvatov.traffic.utils.Pair;

public interface TrafficService {

    /**
     * @return the pair of records from the limits_per_hour table with maximum effective_date value.
     * @throws InvalidLimitsNumberException if there is zero or only one record
     * @throws InvalidLimitsValueException  if min >= max
     */
    Pair<LimitsPerHourEntity, LimitsPerHourEntity> fetchTrafficLimits();

    /**
     * Checks the current amount of transferred data nad if current < limit,
     * then sends specified alert message to specified kafka alert topic.
     *
     * @param current current amount of transferred data
     * @param limits  current limits of transferred data
     * @return true, if transferred traffic is out of range and message was sent, false otherwise.
     */
    boolean validateTrafficAndSendNotification(int current, Pair<LimitsPerHourEntity, LimitsPerHourEntity> limits);
}
