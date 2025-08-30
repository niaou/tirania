package com.niaou.tiraniabot.service;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.springframework.stereotype.Service;

@Service
public class JDAMessagingService implements MessagingService {

  @Override
  public void sendChannelMessage(MessageChannel channel, String message) {
    channel.sendMessage(message).queue();
  }

  @Override
  public void sendChannelMessage(
      MessageChannel channel, MessageCreateData message, Consumer<Message> onSuccess) {
    channel.sendMessage(message).queue(onSuccess);
  }

  @Override
  public void sendChannelEmbedMessage(MessageChannel channel, MessageEmbed build) {
    channel.sendMessageEmbeds(build).queue();
  }

  @Override
  public void sendPrivateMessage(Member member, String message) {
    member
        .getUser()
        .openPrivateChannel()
        .queue(
            privateChannel -> privateChannel.sendMessage(message).queue(),
            _ -> System.out.println("❌ Could not send DM to " + member.getEffectiveName()));
  }

  @Override
  public void sendPrivateOrFallbackMessage(
      Member member, MessageChannel fallbackChannel, String message) {
    member
        .getUser()
        .openPrivateChannel()
        .queue(
            privateChannel -> privateChannel.sendMessage(message).queue(),
            _ ->
                sendChannelMessage(
                    fallbackChannel, "❌ Could not DM " + member.getAsMention() + " — " + message));
  }

  @Override
  public void sendTemporaryMessage(MessageChannel channel, String message, long timeoutSeconds) {
    channel
        .sendMessage(message)
        .queue(msg -> msg.delete().queueAfter(timeoutSeconds, TimeUnit.SECONDS));
  }

  @Override
  public void replyEphemeralMessage(GenericComponentInteractionCreateEvent event, String message) {
    event.reply(message).setEphemeral(true).queue();
  }

  @Override
  public void editEventMessage(GenericComponentInteractionCreateEvent event, String message) {
    event.editMessage(message).setComponents().queue();
  }
}
