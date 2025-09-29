package com.niaou.tiraniabot.command.moderation;

import com.niaou.tiraniabot.command.AbstractCommand;
import com.niaou.tiraniabot.context.Context;
import com.niaou.tiraniabot.service.MessagingService;
import com.niaou.tiraniabot.service.ModService;
import java.util.List;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public abstract class AbstractModCommand extends AbstractCommand {

  private final ModService modService;

  protected AbstractModCommand(
      String name,
      String description,
      int order,
      List<String> aliases,
      MessagingService messagingService,
      ModService modService) {
    super(name, description, order, aliases, messagingService);
    this.modService = modService;
  }

  @Override
  public List<Context> getContexts() {
    return List.of(Context.MOD);
  }

  @Override
  public final void execute(MessageReceivedEvent event, String args) {
    Member member = event.getMember();
    MessageChannel channel = event.getChannel();

    if (member == null || !modService.isModerator(member)) {
      messagingService.sendChannelMessage(
          channel, "🚫 You don’t have permission to use this moderation command!");
      return;
    }

    executeModCommand(event, args);
  }

  protected abstract void executeModCommand(MessageReceivedEvent event, String args);
}
