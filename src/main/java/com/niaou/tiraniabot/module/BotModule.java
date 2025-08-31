package com.niaou.tiraniabot.module;

import lombok.Getter;

@Getter
public enum BotModule {
  GLOBAL("global"),
  MUSIC("music"),
  ADMIN("admin");

  private final String value;

  BotModule(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }
}
