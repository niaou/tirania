package com.niaou.tiraniabot.web;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@AllArgsConstructor
public class VersionController {

  private final BuildProperties buildProperties;
  private final GitProperties gitProperties;

  @GetMapping("/version")
  public Map<String, String> getVersion() {
    return Map.of(
        "version", Optional.ofNullable(buildProperties).map(BuildProperties::getVersion).orElse(""),
        "branch", Optional.ofNullable(gitProperties).map(GitProperties::getBranch).orElse(""),
        "commit",
            Optional.ofNullable(gitProperties).map(GitProperties::getShortCommitId).orElse(""),
        "commitTime",
            Optional.ofNullable(gitProperties)
                .map(GitProperties::getCommitTime)
                .map(Instant::toString)
                .orElse(""));
  }
}
