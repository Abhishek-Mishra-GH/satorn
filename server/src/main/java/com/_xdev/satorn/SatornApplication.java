package com._xdev.satorn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SatornApplication {

	public static void main(String[] args) {
		SpringApplication.run(SatornApplication.class, args);
	}

}