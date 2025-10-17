package ru.home.tasktracker.api.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfiguration {

    @Bean
    public Counter createProjectCounter(MeterRegistry registry) {
        return Counter.builder("project_create_total")
                .description("Count of created projects")
                .register(registry);
    }

}
