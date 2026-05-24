package com.fms.hr_crm.calling;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
public class CallingServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CallingServiceApplication.class, args);
	}
}