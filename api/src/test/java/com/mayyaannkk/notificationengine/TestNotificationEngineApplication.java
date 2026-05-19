package com.mayyaannkk.notificationengine;

import org.springframework.boot.SpringApplication;

public class TestNotificationEngineApplication {

	public static void main(String[] args) {
		SpringApplication.from(NotificationEngineApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
