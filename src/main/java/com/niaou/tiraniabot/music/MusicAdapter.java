package com.niaou.tiraniabot.music;

import com.niaou.tiraniabot.command.CommandHandler;
import com.niaou.tiraniabot.module.ModuleResolver;
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
  private final CommandHandler commandHandler;
  private final ModuleResolver moduleResolver;

  @Override
  public void onMessageReceived(@NotNull MessageReceivedEvent event) {
    if (!event.isFromGuild()
        || event.getAuthor().isBot()
        || !moduleResolver
            .resolveModule(event.getChannel())
            .getValue()
            .equalsIgnoreCase(MUSIC_CHANNEL_NAME)) {
      return;
    }
    commandHandler.handleMessage(event);
  }

  @Override
  public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
    musicService.handleButton(event);
  }
}
