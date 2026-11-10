package com.alphaflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@ConfigurationPropertiesScan
public class AlphaFlowIntradayApp {

  private static final Logger logger = LoggerFactory.getLogger(AlphaFlowIntradayApp.class);

  public static void main(String[] args) {
    logger.info("Starting Alpha Flow Intraday Application...");
    SpringApplication.run(AlphaFlowIntradayApp.class, args);
    logger.info("Alpha Flow Intraday Application started successfully.");
  }
}
