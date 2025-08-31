package com.niaou.tiraniabot.command.music;

import com.niaou.tiraniabot.command.AbstractMusicCommand;
import com.niaou.tiraniabot.music.GuildMusicManager;
import com.niaou.tiraniabot.music.MusicService;
import com.niaou.tiraniabot.service.MessagingService;
import java.util.List;
import lombok.Getter;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.springframework.stereotype.Service;

@Getter
@Service
public class PlayCommand extends AbstractMusicCommand {

  public PlayCommand(MusicService musicService, MessagingService messagingService) {
    super(
        "!play",
        "Play a track or search by keywords",
        10000,
        List.of(),
        messagingService,
        musicService);
  }

  @Override
  public void executeMusicCommand(MessageReceivedEvent event, String args) {
    if (args == null || args.isBlank()) {
      reply(event.getChannel(), "Usage: !play <URL or keywords>");
      return;
    }
    GuildMusicManager manager = getMusicManager(event);
    musicService.play(event, manager, args);
  }
}
