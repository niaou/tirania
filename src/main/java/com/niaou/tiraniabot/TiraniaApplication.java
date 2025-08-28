package com.niaou.tiraniabot;

import net.dv8tion.jda.api.JDABuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.util.StringUtils;

@SpringBootApplication
public class TiraniaApplication implements CommandLineRunner {

  private static final Logger logger = LoggerFactory.getLogger(TiraniaApplication.class);

  @Value("${tirania.token}")
  private String token;

  public static void main(String[] args) {
    SpringApplication.run(TiraniaApplication.class, args);
  }

  @Override
  public void run(String... args) {
    if (!StringUtils.hasLength(token)) {
      logger.error("TiRania token is not set! Please set TIRANIA_TOKEN environment variable");
      throw new IllegalStateException("TiRania token is not set");
    }
    try {
      JDABuilder.createDefault(token).build();
    } catch (Exception e) {
      logger.error("Failed to start TiRania", e);
    }
  }
}
