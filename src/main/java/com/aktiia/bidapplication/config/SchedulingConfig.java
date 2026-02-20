package com.aktiia.bidapplication.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class SchedulingConfig {
    // Enables @Scheduled annotation support for automatic auction expiration
    // TODO: Initial idea only, try to think some better solution, like triggering close event from the database or
    //  using a message queue to handle auction expiration more efficiently and reliably.
}
