package com.niaou.tiraniabot.command;

import com.niaou.tiraniabot.music.GuildMusicManager;
import com.niaou.tiraniabot.music.MusicService;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class PlayMusicCommand implements Command {

  private final MusicService musicService;

  public PlayMusicCommand(MusicService musicService) {
    this.musicService = musicService;
  }

  @Override
  public String getName() {
    return "!play";
  }

  @Override
  public String getDescription() {
    return "Play a track or search by keywords";
  }

  @Override
  public void execute(MessageReceivedEvent event, String args) {
    GuildMusicManager manager =
        musicService.getGuildMusicManager(event.getGuild(), event.getChannel());

    if (args == null || args.isBlank()) {
      musicService
          .getMessagingService()
          .sendChannelMessage(event.getChannel(), "Usage: !play <URL or keywords>");
      return;
    }

    musicService.playMusic(event, manager, args);
  }
}
