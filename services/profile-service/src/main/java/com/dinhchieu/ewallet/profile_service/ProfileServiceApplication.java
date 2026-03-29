package com.dinhchieu.ewallet.profile_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan(basePackages = {
		"com.dinhchieu.ewallet.common_library",
		"com.dinhchieu.ewallet.profile_service"
})
@EnableCaching
public class ProfileServiceApplication {
	public static void main(String[] args) {
		SpringApplication.run(ProfileServiceApplication.class, args);
	}
}
