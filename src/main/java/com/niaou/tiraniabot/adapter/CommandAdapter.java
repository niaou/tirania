package com.niaou.tiraniabot.adapter;

import com.niaou.tiraniabot.command.CommandHandler;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CommandAdapter extends ListenerAdapter {

  private final CommandHandler commandHandler;

  @Override
  public void onMessageReceived(@NotNull MessageReceivedEvent event) {
    if (!event.isFromGuild() || event.getAuthor().isBot()) {
      return;
    }
    commandHandler.handleMessage(event);
  }
}
