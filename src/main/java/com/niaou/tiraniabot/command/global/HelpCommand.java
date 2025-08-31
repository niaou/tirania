package com.niaou.tiraniabot.command.global;

import com.niaou.tiraniabot.command.AbstractCommand;
import com.niaou.tiraniabot.command.Command;
import com.niaou.tiraniabot.command.CommandRegistry;
import com.niaou.tiraniabot.context.Context;
import com.niaou.tiraniabot.context.ContextResolver;
import com.niaou.tiraniabot.service.MessagingService;
import java.awt.Color;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.springframework.stereotype.Service;

@Service
public class HelpCommand extends AbstractCommand {

  private final ContextResolver resolver;

  public HelpCommand(MessagingService messagingService, ContextResolver resolver) {
    super(
        "!help",
        "Show available commands. Use '!help all' for all modules.",
        Integer.MAX_VALUE,
        List.of("!h"),
        messagingService);
    this.resolver = resolver;
  }

  @Override
  public List<Context> getContexts() {
    return List.of(Context.GLOBAL, Context.MUSIC);
  }

  @Override
  public void execute(MessageReceivedEvent event, String args) {
    Context context = resolver.resolveContext(event.getChannel(), args);
    CommandRegistry registry = CommandRegistry.getInstance();

    Collection<Command> commandsToShow;
    if (context == Context.GLOBAL) {
      commandsToShow = registry.getAllCommands();
    } else {
      commandsToShow = registry.getAllContextCommands(context);
    }

    List<Command> sortedCommands =
        commandsToShow.stream()
            .sorted(
                Comparator.comparingInt(
                    cmd -> (cmd instanceof AbstractCommand ac) ? ac.getOrder() : Integer.MAX_VALUE))
            .toList();

    EmbedBuilder embed = new EmbedBuilder();
    embed.setTitle(context == Context.MUSIC ? "🎵 Music Commands" : "Available Commands");
    embed.setColor(context == Context.MUSIC ? Color.CYAN : Color.ORANGE);
    embed.setDescription("Here are the commands you can use:");

    for (Command cmd : sortedCommands) {
      String name = (cmd instanceof AbstractCommand ac) ? ac.getDisplayName() : cmd.getName();
      String contextStr =
          context == Context.GLOBAL
              ? " **["
                  + String.join(", ", cmd.getContexts().stream().map(Context::getValue).toList())
                  + "]**"
              : "";
      embed.addField(name, cmd.getDescription() + contextStr, false);
    }

    messagingService.sendChannelEmbedMessage(event.getChannel(), embed.build());
  }
}
