package com.niaou.tiraniabot.command.global;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.niaou.tiraniabot.command.AbstractCommand;
import com.niaou.tiraniabot.command.Command;
import com.niaou.tiraniabot.command.CommandRegistry;
import com.niaou.tiraniabot.context.Context;
import com.niaou.tiraniabot.context.ContextResolver;
import com.niaou.tiraniabot.service.MessagingService;
import java.awt.*;
import java.util.List;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class HelpCommandTest {

  private MessagingService messagingService;
  private ContextResolver resolver;
  private HelpCommand helpCommand;
  private MessageChannelUnion channel;
  private MessageReceivedEvent event;

  @BeforeEach
  void setup() {
    messagingService = mock(MessagingService.class);
    resolver = mock(ContextResolver.class);

    helpCommand = new HelpCommand(messagingService, resolver);

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

    assertThat(fieldNames).anyMatch(name -> name.contains("!global"));
    assertThat(fieldNames).anyMatch(name -> name.contains("!music"));
    assertThat(fieldNames).anyMatch(name -> name.contains("!alpha"));

    assertThat(fieldNames.get(0)).contains("!alpha");
    assertThat(fieldNames.get(1)).contains("!global");
    assertThat(fieldNames.get(2)).contains("!music");
  }

  @Test
  void execute_inMusicContext_showsOnlyMusicCommands() {
    when(resolver.resolveContext(channel, "")).thenReturn(Context.MUSIC);

    helpCommand.execute(event, "");

    MessageEmbed embed = captureEmbed();

    assertThat(embed.getTitle()).isEqualTo("🎵 Music Commands");
    assertThat(embed.getColor()).isEqualTo(Color.CYAN);

    List<String> fieldNames = embed.getFields().stream().map(MessageEmbed.Field::getName).toList();

    assertThat(fieldNames).anyMatch(name -> name.contains("!music"));
    assertThat(fieldNames).noneMatch(name -> name.contains("!global"));
    assertThat(fieldNames).noneMatch(name -> name.contains("!alpha"));
  }

  @Test
  void execute_inGlobalContext_fieldsContainContextMarkers() {
    when(resolver.resolveContext(channel, "all")).thenReturn(Context.GLOBAL);

    helpCommand.execute(event, "all");

    MessageEmbed embed = captureEmbed();
    List<String> fieldValues =
        embed.getFields().stream().map(MessageEmbed.Field::getValue).toList();

    assertThat(fieldValues).anyMatch(v -> v.contains("[global]"));
    assertThat(fieldValues).anyMatch(v -> v.contains("[music]"));
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
