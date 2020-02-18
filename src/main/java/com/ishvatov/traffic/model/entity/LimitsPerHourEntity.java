package com.ishvatov.traffic.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Date;

/**
 * Basic hibernate entity, which represents the records, that are
 * stored in the limits_per_hour table.
 *
 * @author ishvatov
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "limits_per_hour")
public class LimitsPerHourEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @Column(name = "limit_name")
    private String limitName;

    @Column(name = "limit_value")
    private int limitValue;

    @Column(name = "effective_date")
    private Date effectiveDate;
}
