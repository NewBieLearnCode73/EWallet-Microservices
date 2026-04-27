package com.dinhchieu.ewallet.profile_service.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {
    "com.dinhchieu.ewallet.common_library",
    "com.dinhchieu.ewallet.profile_service"
})
@EnableCaching
public class ProfileServiceConfig {
}
