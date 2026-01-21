package com.paymate.paymate_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableAsync
@SpringBootApplication
@EnableJpaAuditing
public class PaymateServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(PaymateServerApplication.class, args);
	}

}
