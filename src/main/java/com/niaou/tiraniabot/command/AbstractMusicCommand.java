package com.niaou.tiraniabot.command;

import com.niaou.tiraniabot.module.BotModule;
import com.niaou.tiraniabot.music.GuildMusicManager;
import com.niaou.tiraniabot.music.MusicService;
import com.niaou.tiraniabot.service.MessagingService;
import java.util.List;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractMusicCommand extends AbstractCommand {

  private static final String MUSIC_CHANNEL_NAME = "music";
  private static final Logger logger = LoggerFactory.getLogger(AbstractMusicCommand.class);
  protected final MusicService musicService;

  public AbstractMusicCommand(
      String name,
      String description,
      int order,
      List<String> aliases,
      MessagingService messagingService,
      MusicService musicService) {
    super(name, description, order, aliases, messagingService);
    this.musicService = musicService;
  }

  @Override
  public List<BotModule> getModules() {
    return List.of(BotModule.MUSIC);
  }

  protected GuildMusicManager getMusicManager(MessageReceivedEvent event) {
    return musicService.getGuildMusicManager(event.getGuild(), event.getChannel());
  }

  protected abstract void executeMusicCommand(MessageReceivedEvent event, String args);

  @Override
  public final void execute(MessageReceivedEvent event, String args) {
    MessageChannel channel = event.getChannel();
    Member member = event.getMember();
    Guild guild = event.getGuild();

    AudioChannel musicVoiceChannel =
        guild.getVoiceChannelsByName(MUSIC_CHANNEL_NAME, true).stream().findFirst().orElse(null);
    if (musicVoiceChannel == null) {
      logger.error("Music voice channel not defined!");
      messagingService.sendChannelMessage(
          channel, "Music voice channel not defined! Please contact a mod");
      return;
    }

    if (member == null || !ensureUserInMusicVoiceChannel(member, channel, musicVoiceChannel)) {
      return;
    }

    executeMusicCommand(event, args);
  }

  private boolean ensureUserInMusicVoiceChannel(
      Member member, MessageChannel channel, AudioChannel musicAudioChannel) {
    GuildVoiceState userVoiceState = member.getVoiceState();

    if (!member.hasPermission(musicAudioChannel, Permission.VOICE_CONNECT)) {
      messagingService.sendPrivateOrFallbackMessage(
          member,
          channel,
          "🚫 You don’t have permission to join and queue tracks in the music channel. Please contact a mod");
      return false;
    }

    if (userVoiceState == null || !userVoiceState.inAudioChannel()) {
      messagingService.sendTemporaryMessage(
          channel,
          "❌ You need to join the <#"
              + musicAudioChannel.getId()
              + "> voice channel to use music commands!",
          30);
      return false;
    }

    AudioChannel userChannel = userVoiceState.getChannel();

    if (userChannel == null || !userChannel.getName().equalsIgnoreCase(MUSIC_CHANNEL_NAME)) {
      channel
          .sendMessage(
              "🎶 Music commands are only allowed for users joined in the <#"
                  + musicAudioChannel.getId()
                  + "> voice channel.")
          .queue();
      return false;
    }

    return true;
  }
}
