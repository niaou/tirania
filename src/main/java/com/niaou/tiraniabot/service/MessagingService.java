package com.niaou.tiraniabot.service;

import java.util.function.Consumer;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

public interface MessagingService {

  /** Send a message to a public channel. */
  void sendChannelMessage(MessageChannel channel, String message);

  void sendChannelMessage(
      MessageChannel channel, MessageCreateData message, Consumer<Message> onSuccess);

  /** Send an embed message to a public channel. */
  void sendChannelEmbedMessage(MessageChannel channel, MessageEmbed build);

  /** Send a private/direct message to a member. */
  void sendPrivateMessage(Member member, String message);

  /** Try to send a private message, but fallback to channel if DM fails. */
  void sendPrivateOrFallbackMessage(Member member, MessageChannel fallbackChannel, String message);

  /** Send a temporary message (delete after a few seconds). */
  void sendTemporaryMessage(MessageChannel channel, String message, long timeoutSeconds);

  /** Reply an ephemeral message to an event. */
  void replyEphemeralMessage(GenericComponentInteractionCreateEvent event, String message);

  /** Edit an event message. */
  void editEventMessage(GenericComponentInteractionCreateEvent event, String message);
}
