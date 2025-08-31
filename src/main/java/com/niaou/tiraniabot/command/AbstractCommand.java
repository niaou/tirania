package com.niaou.tiraniabot.command;

import com.niaou.tiraniabot.service.MessagingService;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

@RequiredArgsConstructor
@Getter
public abstract class AbstractCommand implements Command {
  private final String name;
  private final String description;
  private final int order;
  private final List<String> aliases;
  protected final MessagingService messagingService;

  protected void reply(MessageChannel channel, String message) {
    messagingService.sendChannelMessage(channel, message);
  }

  public String getDisplayName() {
    if (aliases.isEmpty()) {
      return name;
    }
    return name + " / " + String.join(" / ", aliases);
  }

  @Override
  public List<String> getAliases() {
    return aliases;
  }
}
