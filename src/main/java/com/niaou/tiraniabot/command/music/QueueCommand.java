package com.niaou.tiraniabot.command.music;

import com.niaou.tiraniabot.music.GuildMusicManager;
import com.niaou.tiraniabot.music.MusicService;
import com.niaou.tiraniabot.service.MessagingService;
import java.util.List;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.springframework.stereotype.Service;

@Service
public class QueueCommand extends AbstractMusicCommand {

  public QueueCommand(MusicService musicService, MessagingService messagingService) {
    super(
        "!queue", "Show the current music queue", 60000, List.of(), messagingService, musicService);
  }

  @Override
  public void executeMusicCommand(MessageReceivedEvent event, String args) {
    GuildMusicManager manager = getMusicManager(event);
    musicService.displayQueue(event.getChannel(), manager);
  }
}
