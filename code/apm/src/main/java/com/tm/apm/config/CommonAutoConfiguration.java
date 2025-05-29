package com.tm.apm.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties(TmfConfiguration.class)
@Configuration
@Slf4j
public class CommonAutoConfiguration {
//    @Bean
//    @ConditionalOnMissingBean
//    AbstractService projectManager() {
//        return new ServicesB();
//    }
}