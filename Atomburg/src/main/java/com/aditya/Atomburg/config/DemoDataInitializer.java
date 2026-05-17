package com.aditya.Atomburg.config;

import com.aditya.Atomburg.service.GoalPortalService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DemoDataInitializer {

    @Bean
    public ApplicationRunner seedGoalPortal(GoalPortalService goalPortalService) {
        return args -> goalPortalService.seedDemoData();
    }
}
