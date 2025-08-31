package com.niaou.tiraniabot.command.global;

import com.niaou.tiraniabot.command.AbstractCommand;
import com.niaou.tiraniabot.command.Command;
import com.niaou.tiraniabot.command.CommandRegistry;
import com.niaou.tiraniabot.module.BotModule;
import com.niaou.tiraniabot.module.ModuleResolver;
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

  private final ModuleResolver resolver;

  public HelpCommand(MessagingService messagingService, ModuleResolver resolver) {
    super(
        "!help",
        "Show available commands. Use '!help all' for all modules.",
        Integer.MAX_VALUE,
        List.of("!h"),
        messagingService);
    this.resolver = resolver;
  }

  @Override
  public List<BotModule> getModules() {
    return List.of(BotModule.GLOBAL, BotModule.MUSIC);
  }

  @Override
  public void execute(MessageReceivedEvent event, String args) {
    BotModule module = resolver.resolveModule(event.getChannel(), args);
    CommandRegistry registry = CommandRegistry.getInstance();

    Collection<Command> commandsToShow;
    if (module == BotModule.GLOBAL) {
      commandsToShow = registry.getAllCommands();
    } else {
      commandsToShow = registry.getAllModuleCommands(module);
    }

    List<Command> sortedCommands =
        commandsToShow.stream()
            .sorted(
                Comparator.comparingInt(
                    cmd -> (cmd instanceof AbstractCommand ac) ? ac.getOrder() : Integer.MAX_VALUE))
            .toList();

    EmbedBuilder embed = new EmbedBuilder();
    embed.setTitle(module == BotModule.MUSIC ? "🎵 Music Commands" : "Available Commands");
    embed.setColor(module == BotModule.MUSIC ? Color.CYAN : Color.ORANGE);
    embed.setDescription("Here are the commands you can use:");

    for (Command cmd : sortedCommands) {
      String name = (cmd instanceof AbstractCommand ac) ? ac.getDisplayName() : cmd.getName();
      String modulesStr =
          module == BotModule.GLOBAL
              ? " **["
                  + String.join(", ", cmd.getModules().stream().map(BotModule::getValue).toList())
                  + "]**"
              : "";
      embed.addField(name, cmd.getDescription() + modulesStr, false);
    }

    messagingService.sendChannelEmbedMessage(event.getChannel(), embed.build());
  }
}
