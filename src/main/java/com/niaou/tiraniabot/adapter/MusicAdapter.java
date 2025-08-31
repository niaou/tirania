package com.niaou.tiraniabot.adapter;

import com.niaou.tiraniabot.music.MusicService;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class MusicAdapter extends ListenerAdapter {

  private final MusicService musicService;

  @Override
  public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
    musicService.handleButton(event);
  }
}
