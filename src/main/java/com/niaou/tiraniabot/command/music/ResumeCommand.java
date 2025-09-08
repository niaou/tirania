package com.niaou.tiraniabot.command.music;

import com.niaou.tiraniabot.music.GuildMusicManager;
import com.niaou.tiraniabot.music.MusicService;
import com.niaou.tiraniabot.service.MessagingService;
import java.util.List;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.springframework.stereotype.Service;

@Service
public class ResumeCommand extends AbstractMusicCommand {

  public ResumeCommand(MusicService musicService, MessagingService messagingService) {
    super("!resume", "Resume playback", 50000, List.of(), messagingService, musicService);
  }

  @Override
  public void executeMusicCommand(MessageReceivedEvent event, String args) {
    GuildMusicManager manager = getMusicManager(event);
    musicService.resume(event.getChannel(), manager);
  }
}
