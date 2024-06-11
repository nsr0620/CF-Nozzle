package com.cfnozzle.controller;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/LoggerService")
public class LogLevel {

	// @PostMapping("/setLogger")
	@GetMapping("/setLogger")
	public String setDebugLevel(@RequestParam byte level, @RequestParam String packages) {
		// Initialize variables to store the previous log level and get the logger for
		// the specified package.
		Level prevLevel = null;
		Logger packageLogger = Logger.getLogger(packages);

		// If the package logger's level is not set, set it to INFO.
		if (packageLogger.getLevel() == null)
			packageLogger.setLevel(Level.INFO);

		// Get the previous log level.
		prevLevel = packageLogger.getLevel();

		// Set the new log level based on the input key.
		packageLogger.setLevel(getLevel(level));

		// Return a message indicating the log level change.
		return "Log level for package " + packages + " changed from " + prevLevel + " to " + packageLogger.getLevel()
				+ "";
	}

	/**
	 * This method is used to get Level Object
	 * 
	 * @param level
	 * @return
	 */
	public static Level getLevel(byte key) {

		if (key == 0)
			return Level.OFF;
		else if (key == 1)
			return Level.SEVERE;
		else if (key == 2)
			return Level.WARNING;
		else if (key == 3)
			return Level.INFO;
		else if (key == 4)
			return Level.CONFIG;
		else if (key == 5)
			return Level.ALL;
		// If the input key doesn't match any known log level, return INFO as the
		// default level.
		return Level.INFO;
	}
}
