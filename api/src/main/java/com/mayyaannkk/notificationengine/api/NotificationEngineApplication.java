package com.mayyaannkk.notificationengine.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.mayyaannkk.notificationengine")
@EntityScan(basePackages = "com.mayyaannkk.notificationengine.persistence.entity")
@EnableJpaRepositories(basePackages = "com.mayyaannkk.notificationengine.persistence.repository")
@EnableScheduling
public class NotificationEngineApplication {

	public static void main(String[] args) {
		SpringApplication.run(NotificationEngineApplication.class, args);
	}
}