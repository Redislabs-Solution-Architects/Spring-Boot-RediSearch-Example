package com.redis.example;

import java.util.concurrent.Executor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@SpringBootApplication
@EnableAsync
public class SpringBootRediSearchApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringBootRediSearchApplication.class, args);
	}

	@Bean
	public Executor taskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(1); // single-thread executor
		executor.setMaxPoolSize(1);
		executor.setQueueCapacity(0);
		executor.setThreadNamePrefix("SampleDataGenerator");
		executor.initialize();
		return executor;
	}
}