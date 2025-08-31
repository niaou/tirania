package com.niaou.tiraniabot.command.music;

import com.niaou.tiraniabot.command.AbstractMusicCommand;
import com.niaou.tiraniabot.music.GuildMusicManager;
import com.niaou.tiraniabot.music.MusicService;
import com.niaou.tiraniabot.service.MessagingService;
import java.util.List;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.springframework.stereotype.Service;

@Service
public class NextCommand extends AbstractMusicCommand {

  protected NextCommand(MusicService musicService, MessagingService messagingService) {
    super("!next", "Skip the current track", 3, List.of(), messagingService, musicService);
  }

  @Override
  public void executeMusicCommand(MessageReceivedEvent event, String args) {
    GuildMusicManager manager = getMusicManager(event);
    musicService.skip(event.getChannel(), manager);
  }
}
