package com.niaou.tiraniabot.command.music;

import com.niaou.tiraniabot.music.GuildMusicManager;
import com.niaou.tiraniabot.music.MusicService;
import com.niaou.tiraniabot.service.MessagingService;
import java.util.List;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.springframework.stereotype.Service;

@Service
public class CurrentCommand extends AbstractMusicCommand {

  public CurrentCommand(MusicService musicService, MessagingService messagingService) {
    super(
        "!current",
        "Show the currently playing track",
        20000,
        List.of("!nowplaying", "!np"),
        messagingService,
        musicService);
  }

  @Override
  public void executeMusicCommand(MessageReceivedEvent event, String args) {
    GuildMusicManager manager = getMusicManager(event);
    musicService.current(event.getChannel(), manager);
  }
}
