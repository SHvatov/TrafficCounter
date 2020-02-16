package com.ishvatov.spark.model.repository;

import com.ishvatov.spark.model.entity.LimitsPerHourEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * JPA repository, which is used to manipulate data from limits_per_hour table.
 *
 * @author ishvatov
 */
@Repository
public interface LimitsPerHourRepository extends JpaRepository<LimitsPerHourEntity, String> {
    /**
     * Fetches all the records from the table limits_per_hour where effective_date
     * column has the maximum value and limit_name is max, limited by 1.
     *
     * @return {@link LimitsPerHourEntity} entity.
     */
    @Query(
            nativeQuery = true,
            value = "select *\n" +
                    "from limits_per_hour\n" +
                    "where effective_date = (select max(effective_date) from limits_per_hour)" +
                    "and limit_name = 'max' limit 1"
    )
    Optional<LimitsPerHourEntity> findMaximumLimit();

    /**
     * Fetches all the records from the table limits_per_hour where effective_date
     * column has the maximum value and limit_name is min, limited by 1.
     *
     * @return {@link LimitsPerHourEntity} entity.
     */
    @Query(
            nativeQuery = true,
            value = "select *\n" +
                    "from limits_per_hour\n" +
                    "where effective_date = (select max(effective_date) from limits_per_hour)" +
                    "and limit_name = 'min' limit 1"
    )
    Optional<LimitsPerHourEntity> findMinimumLimit();
}
