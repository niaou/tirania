package com.niaou.tiraniabot.command.global;

import static com.niaou.tiraniabot.context.Context.GLOBAL;
import static com.niaou.tiraniabot.context.Context.MOD;
import static com.niaou.tiraniabot.context.Context.MUSIC;

import com.niaou.tiraniabot.command.AbstractCommand;
import com.niaou.tiraniabot.command.Command;
import com.niaou.tiraniabot.command.CommandRegistry;
import com.niaou.tiraniabot.context.Context;
import com.niaou.tiraniabot.context.ContextResolver;
import com.niaou.tiraniabot.service.MessagingService;
import com.niaou.tiraniabot.service.ModService;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.springframework.stereotype.Service;

@Service
public class HelpCommand extends AbstractCommand {

  private final ContextResolver resolver;
  private final ModService modService;

  public HelpCommand(
      MessagingService messagingService, ContextResolver resolver, ModService modService) {
    super(
        "!help",
        "Show available commands. Use '!help all' for all modules.",
        Integer.MAX_VALUE,
        List.of("!h"),
        messagingService);
    this.resolver = resolver;
    this.modService = modService;
  }

  @Override
  public List<Context> getContexts() {
    return List.of(GLOBAL, MUSIC, MOD);
  }

  @Override
  public void execute(MessageReceivedEvent event, String args) {
    Member member = event.getMember();
    boolean isMod = modService.isModerator(member);

    CommandRegistry registry = CommandRegistry.getInstance();
    List<Command> allCommands = new ArrayList<>(registry.getAllCommands());

    List<Command> commandsToShow = filterCommands(allCommands, args, isMod, event);
    if (commandsToShow.isEmpty()) return;

    Map<Context, List<Command>> grouped = groupAndSortCommandsByContext(commandsToShow);

    EmbedBuilder embed = buildCommandsEmbed(grouped);
    messagingService.sendChannelEmbedMessage(event.getChannel(), embed.build());

    sendModTip(member, args, isMod, resolver.resolveContext(event.getChannel()));
  }

  private List<Command> filterCommands(
      List<Command> commands, String args, boolean isMod, MessageReceivedEvent event) {
    String arg = args == null ? "" : args.trim().toLowerCase();

    return switch (arg) {
      case "all" ->
          commands.stream().filter(cmd -> !cmd.getContexts().contains(Context.MOD)).toList();
      case "mod" -> {
        if (!isMod) {
          messagingService.sendChannelMessage(event.getChannel(), "❌ You are not a moderator.");
          yield List.of();
        }
        yield commands.stream().filter(cmd -> cmd.getContexts().contains(Context.MOD)).toList();
      }
      default -> {
        Context context = resolver.resolveContext(event.getChannel());
        yield commands.stream()
            .filter(cmd -> cmd.getContexts().contains(context))
            .filter(
                cmd -> !(context != Context.GLOBAL && cmd.getContexts().contains(Context.GLOBAL)))
            .toList();
      }
    };
  }

  private Map<Context, List<Command>> groupAndSortCommandsByContext(List<Command> commands) {
    return commands.stream()
        .collect(
            Collectors.groupingBy(
                cmd -> cmd.getContexts().getFirst(),
                LinkedHashMap::new,
                Collectors.collectingAndThen(
                    Collectors.toList(),
                    list ->
                        list.stream()
                            .sorted(
                                Comparator.comparingInt(
                                    c ->
                                        (c instanceof AbstractCommand ac
                                            ? ac.getOrder()
                                            : Integer.MAX_VALUE)))
                            .toList())));
  }

  private EmbedBuilder buildCommandsEmbed(Map<Context, List<Command>> grouped) {
    EmbedBuilder embed = new EmbedBuilder();
    embed.setTitle("Available Commands");
    embed.setColor(Color.ORANGE);
    embed.setDescription("Here are the commands you can use:");

    List<Context> displayOrder = List.of(Context.GLOBAL, MUSIC, Context.MOD);
    for (Context context : displayOrder) {
      List<Command> cmds = grouped.get(context);
      if (cmds == null || cmds.isEmpty()) continue;

      String title =
          switch (context) {
            case MUSIC -> "🎵 Music Commands";
            case MOD -> "🛡 Moderation Commands";
            default -> "📜 General Commands";
          };

      String value =
          cmds.stream()
              .map(
                  cmd ->
                      (cmd instanceof AbstractCommand ac ? ac.getDisplayName() : cmd.getName())
                          + " - "
                          + cmd.getDescription())
              .collect(Collectors.joining("\n"));

      embed.addField(title, value, false);
    }

    return embed;
  }

  private void sendModTip(Member member, String args, boolean isMod, Context context) {
    if (!isMod || !args.isBlank() || MOD.equals(context)) {
      return;
    }

    messagingService.sendPrivateMessageWithTtl(
        member, "💡 Tip: Use `!help mod` to see all moderation commands.", "help-mod-tip");
  }
}
