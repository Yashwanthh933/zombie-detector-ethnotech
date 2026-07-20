package com.zombiedetector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ZombieDetectorApplication {

	public static void main(String[] args) {
		SpringApplication.run(ZombieDetectorApplication.class, args);
	}

}
