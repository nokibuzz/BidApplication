package com.aktiia.bidapplication.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class SchedulingConfig {
    // Left here for the auctions that are not created regularly, but for testing purposes,
    // we will use the @Scheduled annotation in the AuctionService to check for expired auctions every 5 minutes and close them if needed.
}
