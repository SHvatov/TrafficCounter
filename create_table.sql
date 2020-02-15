create schema if not exists traffic_limits;

drop table if exists limits_per_hour;
create table limits_per_hour
(
    id             int primary key not null,
    limit_name     varchar         not null check ( limit_name in ('min', 'max') ),
    limit_value    int             not null check ( limit_value >= 1024 and limit_value <= 1073741824 ),
    effective_date timestamp       not null
);