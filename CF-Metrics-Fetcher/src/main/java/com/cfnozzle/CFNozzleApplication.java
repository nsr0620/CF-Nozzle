package com.cfnozzle;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CFNozzleApplication {

	// Initialize a logger for this class
	private static final Logger logger = Logger.getLogger(CFNozzleApplication.class.getCanonicalName());
		
	public static void main(String[] args) {
		
		createLogsDirectory();
		try (InputStream is = CFNozzleApplication.class.getResourceAsStream("/logging.properties")) {
            LogManager.getLogManager().readConfiguration(is);
        } catch (IOException e) {
        	logger.severe("Error occured while reading logger properties, Exception - " + e);
        }
		// Start the Spring Boot application by running the cfnozzleNozzleApplication class.
		SpringApplication.run(CFNozzleApplication.class, args);
	}
	
	private static void createLogsDirectory() {
        Path path = Paths.get("nozzleLogs");
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } catch (IOException e) {
        	logger.severe("Failed to create logs directory: " + e.getMessage());
        }
    }
}
