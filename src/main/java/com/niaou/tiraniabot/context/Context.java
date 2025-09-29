package com.niaou.tiraniabot.context;

import lombok.Getter;

@Getter
public enum Context {
  GLOBAL("global"),
  MUSIC("music"),
  MOD("moderator"),
  ADMIN("admin");

  private final String value;

  Context(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }
}
