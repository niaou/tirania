package com.niaou.tiraniabot.music;

import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class MusicAdapter extends ListenerAdapter {

  private static final String MUSIC_CHANNEL_NAME = "music";
  private final MusicService musicService;

  @Override
  public void onMessageReceived(@NotNull MessageReceivedEvent event) {
    if (!event.isFromGuild()
        || event.getAuthor().isBot()
        || !event.getChannel().getName().equalsIgnoreCase(MUSIC_CHANNEL_NAME)) return;
    musicService.handleMessage(event);
  }

  @Override
  public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
    musicService.handleButton(event);
  }
}
