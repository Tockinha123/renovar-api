package com.tocka.renovarAPI;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RenovarApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(RenovarApiApplication.class, args);
	}

}
