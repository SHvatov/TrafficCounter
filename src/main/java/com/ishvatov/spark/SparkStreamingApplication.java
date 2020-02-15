package com.ishvatov.spark;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SparkStreamingApplication implements CommandLineRunner {
    public static void main(String[] args) {
        SpringApplication.run(SparkStreamingApplication.class, args);
    }

	@Override
	public void run(String... args) throws Exception {
		return;
	}
}
