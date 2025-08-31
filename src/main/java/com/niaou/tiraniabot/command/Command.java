package com.niaou.tiraniabot.command;

import com.niaou.tiraniabot.module.BotModule;
import java.util.List;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public interface Command {

  String getName();

  String getDescription();

  default List<BotModule> getModules() {
    return List.of(BotModule.GLOBAL);
  }

  default List<String> getAliases() {
    return List.of();
  }

  void execute(MessageReceivedEvent event, String args);
}
