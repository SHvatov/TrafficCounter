package com.ishvatov.spark.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Pair<K, V> {
    private K first;
    private V second;

    public void update(Pair<K, V> other) {
        this.setFirst(other.getFirst());
        this.setSecond(other.getSecond());
    }
}
