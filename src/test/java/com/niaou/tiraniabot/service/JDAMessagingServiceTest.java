package com.niaou.tiraniabot.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Consumer;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import net.dv8tion.jda.api.requests.restaction.CacheRestAction;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.interactions.MessageEditCallbackAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JDAMessagingServiceTest {

  private JDAMessagingService messagingService;

  @BeforeEach
  void setUp() {
    messagingService = new JDAMessagingService();
  }

  @Test
  void sendChannelMessage_sendsTextMessage() {
    MessageChannel channel = mock(MessageChannel.class);
    when(channel.sendMessage("hello")).thenReturn(mock(MessageCreateAction.class));

    messagingService.sendChannelMessage(channel, "hello");

    verify(channel).sendMessage("hello");
  }

  @Test
  void sendChannelMessage_withConsumer() {
    MessageChannel channel = mock(MessageChannel.class);
    MessageCreateAction action = mock(MessageCreateAction.class);
    when(channel.sendMessage(any(MessageCreateData.class))).thenReturn(action);

    Consumer<Message> consumer = mock(Consumer.class);

    messagingService.sendChannelMessage(channel, MessageCreateData.fromContent("yo"), consumer);

    verify(channel).sendMessage(any(MessageCreateData.class));
    verify(action).queue(eq(consumer));
  }

  @Test
  void sendChannelEmbedMessage_sendsEmbed() {
    MessageChannel channel = mock(MessageChannel.class);
    when(channel.sendMessageEmbeds(any(MessageEmbed.class)))
        .thenReturn(mock(MessageCreateAction.class));

    messagingService.sendChannelEmbedMessage(channel, mock(MessageEmbed.class));

    verify(channel).sendMessageEmbeds(any(MessageEmbed.class));
  }

  @Test
  void sendPrivateMessage_success() {
    Member member = mock(Member.class);
    User user = mock(User.class);
    when(member.getUser()).thenReturn(user);

    mock(PrivateChannel.class);
    when(user.openPrivateChannel()).thenReturn(mock(CacheRestAction.class));

    messagingService.sendPrivateMessage(member, "dm");

    verify(user).openPrivateChannel();
  }

  @Test
  void sendPrivateOrFallbackMessage_fallbackWhenDMFails() {
    Member member = mock(Member.class);
    User user = mock(User.class);
    when(member.getUser()).thenReturn(user);
    when(member.getAsMention()).thenReturn("@user");

    MessageChannel fallbackChannel = mock(MessageChannel.class);
    when(fallbackChannel.sendMessage(anyString())).thenReturn(mock(MessageCreateAction.class));

    CacheRestAction<PrivateChannel> restAction = mock(CacheRestAction.class);
    when(user.openPrivateChannel()).thenReturn(restAction);

    doAnswer(
            inv -> {
              var onError = (java.util.function.Consumer<Throwable>) inv.getArgument(1);
              onError.accept(new RuntimeException("fail"));
              return null;
            })
        .when(restAction)
        .queue(any(), any());

    messagingService.sendPrivateOrFallbackMessage(member, fallbackChannel, "msg");

    verify(fallbackChannel).sendMessage(contains("❌ Could not DM"));
  }

  @Test
  void sendTemporaryMessage_deletesAfterTimeout() {
    MessageChannel channel = mock(MessageChannel.class);
    Message message = mock(Message.class);
    MessageCreateAction action = mock(MessageCreateAction.class);

    when(channel.sendMessage("temp")).thenReturn(action);
    doAnswer(
            inv -> {
              var successConsumer = (Consumer<Message>) inv.getArgument(0);
              successConsumer.accept(message);
              return null;
            })
        .when(action)
        .queue(any());

    when(message.delete()).thenReturn(mock(AuditableRestAction.class));

    messagingService.sendTemporaryMessage(channel, "temp", 5);

    verify(message).delete();
  }

  @Test
  void replyEphemeralMessage_sendsEphemeral() {
    GenericComponentInteractionCreateEvent event =
        mock(GenericComponentInteractionCreateEvent.class);
    ReplyCallbackAction replyAction = mock(ReplyCallbackAction.class);
    when(event.reply("ephemeral")).thenReturn(replyAction);
    when(replyAction.setEphemeral(true)).thenReturn(replyAction);

    messagingService.replyEphemeralMessage(event, "ephemeral");

    verify(replyAction).queue();
  }

  @Test
  void editEventMessage_editsMessage() {
    GenericComponentInteractionCreateEvent event =
        mock(GenericComponentInteractionCreateEvent.class);
    MessageEditCallbackAction action = mock(MessageEditCallbackAction.class);

    when(event.editMessage("updated")).thenReturn(action);
    when(action.setComponents()).thenReturn(action);

    messagingService.editEventMessage(event, "updated");

    verify(action).queue();
  }
}
