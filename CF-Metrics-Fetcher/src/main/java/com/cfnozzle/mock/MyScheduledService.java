package com.cfnozzle.mock;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class MyScheduledService {

    @Scheduled(fixedRate = 10) // 15 minutes = 900,000 milliseconds900_000
    public void performScheduledTask() {
        // Place your code here to be executed every 15 minutes
        // This method will be called automatically at the specified interval.
    	
    }
}
