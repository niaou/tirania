package com.niaou.tiraniabot;

import java.util.List;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.util.StringUtils;

@SpringBootApplication(scanBasePackages = {"com.niaou.tiraniabot"})
public class TiraniaApplication implements CommandLineRunner {

  private static final Logger logger = LoggerFactory.getLogger(TiraniaApplication.class);

  private final List<ListenerAdapter> adapters;

  @Value("${tirania.token}")
  private String token;

  public TiraniaApplication(List<ListenerAdapter> adapters) {
    this.adapters = adapters;
  }

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
      JDABuilder builder =
          JDABuilder.createDefault(token)
              .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT);
      adapters.forEach(builder::addEventListeners);
      builder.build();
    } catch (Exception e) {
      logger.error("Failed to start TiRania", e);
    }
  }
}
