package com.niaou.tiraniabot;

import org.junit.jupiter.api.Test;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class TiraniaApplicationContextTest {

  /** Disable the real CommandLineRunner so JDA does not start in tests. */
  @TestConfiguration
  static class DisableRunnerConfig {
    @Bean
    CommandLineRunner noOpRunner() {
      return _ -> {};
    }
  }

  @Test
  void contextLoads() {
    // verifies that Spring context can start
  }
}
