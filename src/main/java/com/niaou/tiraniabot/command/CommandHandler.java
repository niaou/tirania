package com.niaou.tiraniabot.command;

import com.niaou.tiraniabot.module.BotModule;
import com.niaou.tiraniabot.module.ModuleResolver;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CommandHandler {

  private final CommandRegistry registry;
  private final ModuleResolver resolver;

  public void handleMessage(MessageReceivedEvent event) {
    String[] parts = event.getMessage().getContentRaw().split(" ", 2);
    String commandName = parts[0].toLowerCase();
    String args = parts.length > 1 ? parts[1] : "";

    BotModule module = resolver.resolveModule(event.getChannel());
    Command command = registry.getCommand(module, commandName);

    if (command != null) {
      command.execute(event, args);
    }
  }
}
