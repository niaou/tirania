package com.niaou.tiraniabot.adapter;

import com.niaou.tiraniabot.handler.MusicSendHandler;
import com.niaou.tiraniabot.manager.GuildMusicManager;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class MusicAdapter extends ListenerAdapter {

  private final AudioPlayerManager playerManager;
  private final Map<Long, GuildMusicManager> musicManagers = new HashMap<>();

  public MusicAdapter() {
    this.playerManager = new DefaultAudioPlayerManager();
    AudioSourceManagers.registerRemoteSources(playerManager);
    AudioSourceManagers.registerLocalSource(playerManager);
    YoutubeAudioSourceManager youtubeManager = new YoutubeAudioSourceManager(true);
    youtubeManager.useOauth2(null, false);
    playerManager.registerSourceManager(youtubeManager);
  }

  @Override
  public void onMessageReceived(@NotNull MessageReceivedEvent event) {
    if (!event.isFromGuild() || event.getAuthor().isBot() || !event.getChannel().getName().equalsIgnoreCase("music")) {
      return;
    }

    String msg = event.getMessage().getContentRaw();
    String[] parts = msg.split(" ", 2);
    String command = parts[0].toLowerCase();

    Guild guild = event.getGuild();
    GuildMusicManager musicManager = getGuildMusicManager(guild);

    switch (command) {
      case "!play" -> playMusic(event, msg, musicManager);
      case "!next" -> nextTrackInQueue(event, musicManager);
      case "!queue" -> displayQueue(event, musicManager);
      case "!pause" -> pauseMusic(event, musicManager);
      case "!resume" -> resumeMusic(event, musicManager);
      case "!stop" -> stopAndClear(guild, event, musicManager);
      case "!nowplaying" -> displayPlayingTrack(event, musicManager);
      default -> {
        //ignore
      }
    }
  }

  private void stopAndClear(Guild guild, MessageReceivedEvent event, GuildMusicManager musicManager) {
    musicManager.stopAndClear();

    if (guild.getAudioManager().isConnected()) {
      guild.getAudioManager().closeAudioConnection();
    }

    event.getChannel().sendMessage("⏹ Stopped playback and cleared the queue!").queue();
  }

  private GuildMusicManager getGuildMusicManager(Guild guild) {
    return musicManagers.computeIfAbsent(guild.getIdLong(), id -> {
      AudioPlayer player = playerManager.createPlayer();
      GuildMusicManager manager = new GuildMusicManager(player);
      guild.getAudioManager().setSendingHandler(new MusicSendHandler(player));
      return manager;
    });
  }

  private void displayQueue(@NotNull MessageReceivedEvent event, GuildMusicManager musicManager) {
    if (musicManager.queue.isEmpty() && musicManager.getPlayingTrack() == null) {
      event.getChannel().sendMessage("The queue is empty!").queue();
      return;
    }

    StringBuilder sb = new StringBuilder();
    AudioTrack current = musicManager.getPlayingTrack();
    if (current != null) {
      sb.append("**Now playing:** ").append(current.getInfo().title).append("\n");
    }

    if (!musicManager.queue.isEmpty()) {
      sb.append("**Up next:**\n");
      int i = 1;
      for (AudioTrack track : musicManager.queue) {
        sb.append(i++).append(". ").append(track.getInfo().title).append("\n");
      }
    }

    event.getChannel().sendMessage(sb.toString()).queue();
  }

  private void nextTrackInQueue(@NotNull MessageReceivedEvent event, GuildMusicManager musicManager) {
    if (!musicManager.queue.isEmpty()) {
      musicManager.nextTrack();
      event.getChannel().sendMessage("⏭ Skipped to next track!").queue();
    } else {
      event.getChannel().sendMessage("No track in the queue!").queue();
    }
  }

  private void playMusic(@NotNull MessageReceivedEvent event, String msg, GuildMusicManager musicManager) {
    String[] parts = msg.split(" ", 2);
    if (parts.length < 2) {
      event.getChannel().sendMessage("Usage: !play <URL>").queue();
      return;
    }
    String url = parts[1];
    loadAndQueue(event, musicManager, url);
  }

  private void resumeMusic(@NotNull MessageReceivedEvent event, GuildMusicManager musicManager) {
    if (musicManager.getPlayingTrack() != null && musicManager.isPaused()) {
      musicManager.resume();
      event.getChannel().sendMessage("▶ Resumed!").queue();
    } else {
      event.getChannel().sendMessage("No track is paused!").queue();
    }
  }

  private void pauseMusic(@NotNull MessageReceivedEvent event, GuildMusicManager musicManager) {
    if (musicManager.getPlayingTrack() != null && !musicManager.isPaused()) {
      musicManager.pause();
      event.getChannel().sendMessage("⏸ Paused!").queue();
    } else {
      event.getChannel().sendMessage("No track is playing or already paused!").queue();
    }
  }

  private static void displayPlayingTrack(@NotNull MessageReceivedEvent event, GuildMusicManager musicManager) {
    AudioTrack current = musicManager.getPlayingTrack();
    if (current != null) {
      event.getChannel().sendMessage("Now playing: " + current.getInfo().title).queue();
    } else {
      event.getChannel().sendMessage("Nothing is playing currently!").queue();
    }
  }

  private void loadAndQueue(MessageReceivedEvent event, GuildMusicManager musicManager, String url) {
    Guild guild = event.getGuild();
    List<VoiceChannel> channels = guild.getVoiceChannelsByName("music", true);
    VoiceChannel channel = channels.getFirst();
    if (channel == null) {
      event.getChannel().sendMessage("No 'music' voice channel found!").queue();
      return;
    }

    // Connect if not already
    if (!guild.getAudioManager().isConnected()) {
      guild.getAudioManager().openAudioConnection(channel);
    }

    playerManager.loadItemOrdered(musicManager, url, new AudioLoadResultHandler() {
      @Override
      public void trackLoaded(AudioTrack track) {
        if (musicManager.queue.isEmpty() && musicManager.getPlayingTrack() == null) {
          event.getChannel().sendMessage("Playing: " + track.getInfo().title).queue();
        } else {
          event.getChannel().sendMessage("Queued: " + track.getInfo().title).queue();
        }
        musicManager.queue(track);
      }

      @Override
      public void playlistLoaded(AudioPlaylist playlist) {
        playlist.getTracks().forEach(musicManager::queue);
        event.getChannel().sendMessage("Playlist queued!").queue();
      }

      @Override
      public void noMatches() {
        event.getChannel().sendMessage("No track found for URL: " + url).queue();
      }

      @Override
      public void loadFailed(FriendlyException exception) {
        event.getChannel().sendMessage("Failed to load track: " + exception.getMessage()).queue();
      }
    });

  }

}
