package com.niaou.tiraniabot.command;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.niaou.tiraniabot.context.Context;
import com.niaou.tiraniabot.context.ContextResolver;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommandHandlerTest {

  @Mock private CommandRegistry registry;
  @Mock private ContextResolver resolver;
  @Mock private Command command;
  @Mock private MessageReceivedEvent event;
  @Mock private Message message;
  @Mock private MessageChannelUnion channel;

  @InjectMocks private CommandHandler handler;

  @Test
  void handleMessage_executesCommand_whenFound() {
    when(event.getMessage()).thenReturn(message);
    when(message.getContentRaw()).thenReturn("!play song123");
    when(event.getChannel()).thenReturn(channel);
    when(resolver.resolveContext(channel)).thenReturn(Context.MUSIC);
    when(registry.getCommand(Context.MUSIC, "!play")).thenReturn(command);

    handler.handleMessage(event);

    verify(command).execute(event, "song123");
  }

  @Test
  void handleMessage_doesNothing_whenWrongCommand() {
    when(event.getMessage()).thenReturn(message);
    when(message.getContentRaw()).thenReturn("!unknown");
    when(event.getChannel()).thenReturn(channel);
    when(resolver.resolveContext(channel)).thenReturn(Context.MUSIC);
    when(registry.getCommand(Context.MUSIC, "!unknown")).thenReturn(null);

    handler.handleMessage(event);

    verifyNoInteractions(command);
  }
}
