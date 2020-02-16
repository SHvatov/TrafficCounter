package com.ishvatov.spark.exception;

/**
 * Wrapper, which is used to wrap checked Pcap exceptions.
 *
 * @author ishvatov
 */
public class PcapRuntimeException extends RuntimeException {
    public PcapRuntimeException(Throwable throwable) {
        super(throwable);
    }
}
