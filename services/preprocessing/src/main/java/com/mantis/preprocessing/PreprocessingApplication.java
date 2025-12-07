package com.mantis.preprocessing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PreprocessingApplication {

	public static void main(String[] args) {
		SpringApplication.run(PreprocessingApplication.class, args);
	}

}
