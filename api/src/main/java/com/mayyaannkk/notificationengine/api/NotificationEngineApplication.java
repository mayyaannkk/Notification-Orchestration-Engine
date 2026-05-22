package com.mayyaannkk.notificationengine.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.mayyaannkk.notificationengine")
@EntityScan(basePackages = "com.mayyaannkk.notificationengine.persistence.entity")
@EnableJpaRepositories(basePackages = "com.mayyaannkk.notificationengine.persistence.repository")
public class NotificationEngineApplication {

	public static void main(String[] args) {
		SpringApplication.run(NotificationEngineApplication.class, args);
	}
}