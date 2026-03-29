package com.dinhchieu.ewallet.transaction_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@ComponentScan(basePackages = {
		"com.dinhchieu.ewallet.common_library",
		"com.dinhchieu.ewallet.transaction_service"
})
@EnableScheduling
@EnableJpaRepositories(basePackages = "com.dinhchieu.ewallet.transaction_service.repositories.jpa")
@EnableMongoRepositories(basePackages = "com.dinhchieu.ewallet.transaction_service.repositories.mongodb")
public class TransactionServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(TransactionServiceApplication.class, args);
	}

}
