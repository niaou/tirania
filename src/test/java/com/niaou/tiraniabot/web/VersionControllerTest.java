package com.niaou.tiraniabot.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;

class VersionControllerTest {

  private BuildProperties buildProperties;
  private GitProperties gitProperties;
  private VersionController controller;

  @BeforeEach
  void setUp() {
    buildProperties = mock(BuildProperties.class);
    gitProperties = mock(GitProperties.class);
    controller = new VersionController(buildProperties, gitProperties);
  }

  @Test
  void getVersion_returnsExpectedValues() {
    when(buildProperties.getVersion()).thenReturn("1.2.3");
    when(gitProperties.getBranch()).thenReturn("main");
    when(gitProperties.getShortCommitId()).thenReturn("abcd123");
    when(gitProperties.getCommitTime()).thenReturn(Instant.parse("2025-08-31T20:00:00Z"));

    Map<String, String> result = controller.getVersion();

    assertThat(result)
        .containsEntry("version", "1.2.3")
        .containsEntry("branch", "main")
        .containsEntry("commit", "abcd123")
        .containsEntry("commitTime", "2025-08-31T20:00:00Z");
  }

  @Test
  void getVersion_handlesNullProperties() {
    VersionController nullController = new VersionController(null, null);
    Map<String, String> result = nullController.getVersion();

    assertThat(result)
        .containsEntry("version", "")
        .containsEntry("branch", "")
        .containsEntry("commit", "")
        .containsEntry("commitTime", "");
  }
}
