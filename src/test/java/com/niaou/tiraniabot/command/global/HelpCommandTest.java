package com.niaou.tiraniabot.command.global;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.niaou.tiraniabot.command.AbstractCommand;
import com.niaou.tiraniabot.command.Command;
import com.niaou.tiraniabot.command.CommandRegistry;
import com.niaou.tiraniabot.context.Context;
import com.niaou.tiraniabot.context.ContextResolver;
import com.niaou.tiraniabot.service.MessagingService;
import com.niaou.tiraniabot.service.ModService;
import java.awt.*;
import java.util.List;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class HelpCommandTest {

  private MessagingService messagingService;
  private ContextResolver resolver;
  private HelpCommand helpCommand;
  private MessageChannelUnion channel;
  private MessageReceivedEvent event;
  private ModService modService;

  @BeforeEach
  void setup() {
    messagingService = mock(MessagingService.class);
    resolver = mock(ContextResolver.class);
    modService = mock(ModService.class);

    helpCommand = new HelpCommand(messagingService, resolver, modService);

    channel = mock(MessageChannelUnion.class);
    event = mock(MessageReceivedEvent.class);
    when(event.getChannel()).thenReturn(channel);

    // Dummy commands with different contexts & order
    Command globalCommand =
        new DummyCommand("!global", "Global test command", Context.GLOBAL, 1, "!g");
    Command musicCommand = new DummyCommand("!music", "Music test command", Context.MUSIC, 2, "!m");
    Command anotherGlobalCommand =
        new DummyCommand("!alpha", "Another global command", Context.GLOBAL, 0);

    // Reset the registry
    CommandRegistry.reset();
    CommandRegistry.getInstance().register(globalCommand);
    CommandRegistry.getInstance().register(musicCommand);
    CommandRegistry.getInstance().register(anotherGlobalCommand);
  }

  @Test
  void execute_inGlobalContext_showsAllCommands() {
    when(resolver.resolveContext(channel, "all")).thenReturn(Context.GLOBAL);

    helpCommand.execute(event, "all");

    MessageEmbed embed = captureEmbed();

    assertThat(embed.getTitle()).isEqualTo("Available Commands");
    assertThat(embed.getColor()).isEqualTo(Color.ORANGE);
    assertThat(embed.getDescription()).contains("Here are the commands");

    List<String> fieldNames = embed.getFields().stream().map(MessageEmbed.Field::getName).toList();

    assertThat(fieldNames.get(0)).contains("General Commands");
    assertThat(fieldNames.get(1)).contains("Music Commands");

    List<String> fieldValues =
        embed.getFields().stream().map(MessageEmbed.Field::getValue).toList();

    assertThat(fieldValues.get(0)).contains("!alpha");
    assertThat(fieldValues.get(0)).contains("!global");
    assertThat(fieldValues.get(1)).contains("!music");
  }

  @Test
  void execute_inMusicContext_showsOnlyMusicCommands() {
    when(resolver.resolveContext(channel, "")).thenReturn(Context.MUSIC);
    when(resolver.resolveContext(channel)).thenReturn(Context.MUSIC);
    helpCommand.execute(event, "");

    MessageEmbed embed = captureEmbed();

    assertThat(embed.getTitle()).isEqualTo("Available Commands");
    assertThat(embed.getColor()).isEqualTo(Color.ORANGE);

    List<String> fieldNames = embed.getFields().stream().map(MessageEmbed.Field::getName).toList();

    assertThat(fieldNames.getFirst()).contains("Music Commands");
    assertThat(fieldNames).noneMatch(name -> name.contains("General Commands"));

    List<String> fieldValues =
        embed.getFields().stream().map(MessageEmbed.Field::getValue).toList();

    assertThat(fieldValues).anyMatch(name -> name.contains("!music"));
    assertThat(fieldValues).noneMatch(name -> name.contains("!global"));
    assertThat(fieldValues).noneMatch(name -> name.contains("!alpha"));
  }

  @Test
  void execute_inGlobalContext_fieldsContainContextMarkers() {
    when(resolver.resolveContext(channel, "all")).thenReturn(Context.GLOBAL);

    helpCommand.execute(event, "all");

    MessageEmbed embed = captureEmbed();
    List<String> fieldNames = embed.getFields().stream().map(MessageEmbed.Field::getName).toList();

    assertThat(fieldNames.get(0)).contains("General Commands");
    assertThat(fieldNames.get(1)).contains("Music Commands");

    List<String> fieldValues =
        embed.getFields().stream().map(MessageEmbed.Field::getValue).toList();

    assertThat(fieldValues.get(0)).contains("!global");
    assertThat(fieldValues.get(1)).contains("!music");
    assertThat(fieldValues.get(1)).contains("!m");
  }

  @Test
  void execute_inGlobalContext_commandsAreSortedByOrder() {
    when(resolver.resolveContext(channel, "all")).thenReturn(Context.GLOBAL);

    helpCommand.execute(event, "all");
    MessageEmbed embed = captureEmbed();

    String generalField =
        embed.getFields().stream()
            .filter(
                f -> {
                  Assertions.assertNotNull(f.getName());
                  return f.getName().contains("General Commands");
                })
            .findFirst()
            .map(MessageEmbed.Field::getValue)
            .orElse("");

    // "!alpha" (order 0) should come before "!global" (order 1)
    assertThat(generalField.indexOf("!alpha")).isLessThan(generalField.indexOf("!global"));
  }

  @Test
  void execute_inModContext_showsModCommandsOnlyForMods() {
    Command modCommand = new DummyCommand("!kick", "Kick users", Context.MOD, 5, "!k");
    CommandRegistry.getInstance().register(modCommand);

    Member member = mock(Member.class);
    when(event.getMember()).thenReturn(member);
    when(resolver.resolveContext(channel, "mod")).thenReturn(Context.MOD);

    // Non-mod should get empty embed
    when(modService.isModerator(member)).thenReturn(false);
    helpCommand.execute(event, "mod");
    verify(messagingService).sendChannelMessage(eq(channel), eq("❌ You are not a moderator."));

    // Mod should see the command
    reset(messagingService);
    when(modService.isModerator(member)).thenReturn(true);
    helpCommand.execute(event, "mod");
    MessageEmbed embedMod = captureEmbed();
    assertThat(embedMod.getFields())
        .anyMatch(
            f -> {
              Assertions.assertNotNull(f.getValue());
              return f.getValue().contains("!kick");
            });
  }

  @Test
  void execute_withUnknownArgs_defaultsToContextFromResolver() {
    when(resolver.resolveContext(channel, "weird")).thenReturn(Context.MUSIC);
    when(resolver.resolveContext(channel)).thenReturn(Context.MUSIC);

    helpCommand.execute(event, "weird");

    MessageEmbed embed = captureEmbed();
    assertThat(embed.getFields())
        .anyMatch(
            f -> {
              Assertions.assertNotNull(f.getName());
              return f.getName().contains("Music Commands");
            });
  }

  @Test
  void execute_withNoCommands_doesNotSendEmbed() {
    CommandRegistry.reset(); // no commands
    when(resolver.resolveContext(channel, "all")).thenReturn(Context.GLOBAL);

    helpCommand.execute(event, "all");

    verifyNoInteractions(messagingService);
  }

  @Test
  void execute_inGlobalContext_tipIsSentToModsOnly() {
    Member modMember = mock(Member.class);
    when(event.getMember()).thenReturn(modMember);
    when(modService.isModerator(modMember)).thenReturn(true);
    when(resolver.resolveContext(channel, "")).thenReturn(Context.GLOBAL);
    when(resolver.resolveContext(channel)).thenReturn(Context.GLOBAL);

    helpCommand.execute(event, "");

    // Verify private message was attempted
    verify(messagingService)
        .sendPrivateMessageWithTtl(eq(modMember), contains("Use `!help mod`"), eq("help-mod-tip"));
  }

  private MessageEmbed captureEmbed() {
    ArgumentCaptor<MessageEmbed> captor = ArgumentCaptor.forClass(MessageEmbed.class);
    verify(messagingService).sendChannelEmbedMessage(eq(channel), captor.capture());
    return captor.getValue();
  }

  // Dummy command implementation
  private static class DummyCommand extends AbstractCommand {
    private final List<Context> contexts;

    DummyCommand(String name, String description, Context context, int order, String... aliases) {
      super(name, description, order, List.of(aliases), mock(MessagingService.class));
      this.contexts = List.of(context);
    }

    @Override
    public List<Context> getContexts() {
      return contexts;
    }

    @Override
    public void execute(MessageReceivedEvent event, String args) {}
  }
}
