package com.niaou.tiraniabot.music;

import com.niaou.tiraniabot.service.MessagingService;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MusicService {

  private final AudioPlayerManager playerManager;
  private final Map<Long, GuildMusicManager> musicManagers = new ConcurrentHashMap<>();
  private final Map<Long, SelectionData> pendingSelections = new ConcurrentHashMap<>();
  private final MessagingService messagingService;
  private static final String MUSIC_CHANNEL_NAME = "music";
  private static final Logger logger = LoggerFactory.getLogger(MusicService.class);

  public MusicService(MessagingService messagingService) {
    this.messagingService = messagingService;
    this.playerManager = new DefaultAudioPlayerManager();
    YoutubeAudioSourceManager youtubeManager = new YoutubeAudioSourceManager(true);
    youtubeManager.useOauth2(null, false);
    playerManager.registerSourceManager(youtubeManager);
    AudioSourceManagers.registerRemoteSources(playerManager);
    AudioSourceManagers.registerLocalSource(playerManager);
  }

  public void handleMessage(MessageReceivedEvent event) {
    String msg = event.getMessage().getContentRaw();
    String[] parts = msg.split(" ", 2);
    String command = parts[0].toLowerCase();

    MessageChannel channel = event.getChannel();
    Guild guild = event.getGuild();
    GuildMusicManager musicManager = getGuildMusicManager(guild, channel);

    AudioChannel musicVoiceChannel =
        guild.getVoiceChannelsByName(MUSIC_CHANNEL_NAME, true).stream().findFirst().orElse(null);

    if (musicVoiceChannel == null) {
      logger.error("Music voice channel not defined!");
      messagingService.sendChannelMessage(
          channel, "Music voice channel not defined! Please contact a mod");
      return;
    }

    if (event.getMember() == null
        || !ensureUserInMusicVoiceChannel(event.getMember(), channel, musicVoiceChannel)) {
      return;
    }

    switch (command) {
      case "!play" -> {
        if (parts.length < 2) {
          messagingService.sendChannelMessage(channel, "Usage: !play <URL or keywords>");
          return;
        }
        playMusic(event, musicManager, parts[1]);
      }
      case "!next" -> skip(channel, musicManager);
      case "!queue" -> displayQueue(channel, musicManager);
      case "!pause" -> pause(channel, musicManager);
      case "!resume" -> resume(channel, musicManager);
      case "!stop" -> stopAndClear(channel, guild, musicManager);
      case "!nowplaying" -> nowPlaying(event.getChannel(), musicManager);
      case "!help" -> help(channel);
      default -> {
        // ignore
      }
    }
  }

  public void handleButton(ButtonInteractionEvent event) {
    long messageId = event.getMessageIdLong();
    SelectionData data = pendingSelections.remove(messageId);
    if (data == null) {
      return;
    }

    int index;
    try {
      index = Integer.parseInt(event.getComponentId().split("_")[1]);
    } catch (NumberFormatException e) {
      messagingService.replyEphemeralMessage(event, "Invalid selection!");
      return;
    }

    if (index < 0 || index >= data.tracks.size()) {
      messagingService.replyEphemeralMessage(event, "Selection out of bounds!");
      return;
    }

    AudioTrack track = data.tracks.get(index);
    data.manager.queue(track);

    String newContent =
        event.getMessage().getContentRaw() + "\n\n✅ Selected: " + track.getInfo().title;

    messagingService.editEventMessage(event, newContent);
  }

  public GuildMusicManager getGuildMusicManager(Guild guild, MessageChannel channel) {
    return musicManagers.computeIfAbsent(
        guild.getIdLong(),
        id -> {
          var player = playerManager.createPlayer();
          var manager =
              new GuildMusicManager(player, new LinkedList<>(), channel, messagingService);

          AudioManager audioManager = guild.getAudioManager();
          audioManager.setSendingHandler(new MusicSendHandler(player));
          return manager;
        });
  }

  public void playMusic(MessageReceivedEvent event, GuildMusicManager musicManager, String query) {
    Guild guild = event.getGuild();
    MessageChannel channel = event.getChannel();
    List<VoiceChannel> channels = guild.getVoiceChannelsByName(MUSIC_CHANNEL_NAME, true);
    if (channels.isEmpty()) {
      messagingService.sendChannelMessage(
          channel, "No '" + MUSIC_CHANNEL_NAME + "' voice channel found!");
      return;
    }

    AudioManager audioManager = guild.getAudioManager();
    if (!audioManager.isConnected()) {
      audioManager.openAudioConnection(channels.getFirst());
    }

    String loadQuery = query.startsWith("http") ? query : "ytsearch:" + query;

    playerManager.loadItemOrdered(
        musicManager,
        loadQuery,
        new AudioLoadResultHandler() {
          @Override
          public void trackLoaded(AudioTrack track) {
            musicManager.queue(track);
            event
                .getChannel()
                .sendMessage(
                    (musicManager.getPlayingTrack() == track ? "🎵 Now playing: " : "➕ Queued: ")
                        + track.getInfo().title)
                .queue();
          }

          @Override
          public void playlistLoaded(AudioPlaylist playlist) {
            if (!playlist.isSearchResult()) {
              playlist.getTracks().forEach(musicManager::queue);
              messagingService.sendChannelMessage(
                  channel,
                  "📜 Playlist queued! Total tracks added: " + playlist.getTracks().size());
            } else {
              List<AudioTrack> topTracks =
                  playlist.getTracks().subList(0, Math.min(5, playlist.getTracks().size()));
              sendSelectionMessage(channel, musicManager, topTracks);
            }
          }

          @Override
          public void noMatches() {
            messagingService.sendChannelMessage(channel, "❌ No results found for: " + query);
          }

          @Override
          public void loadFailed(FriendlyException exception) {
            messagingService.sendChannelMessage(
                channel, "⚠ Failed to load track: " + exception.getMessage());
          }
        });
  }

  private void skip(MessageChannel channel, GuildMusicManager manager) {
    if (manager.queue.isEmpty()) {
      messagingService.sendChannelMessage(channel, "No track in the queue!");
    } else {
      AudioTrack next = manager.queue.peek();
      messagingService.sendChannelMessage(
          channel, "⏭ Skipped to next track \n🎵 Now playing: " + next.getInfo().title);
      manager.nextTrack();
    }
  }

  private void pause(MessageChannel channel, GuildMusicManager manager) {
    if (manager.getPlayingTrack() != null && !manager.isPaused()) {
      manager.pause();
      messagingService.sendChannelMessage(channel, "⏸ Paused!");
    } else {
      messagingService.sendChannelMessage(channel, "No track is playing or already paused!");
    }
  }

  private void resume(MessageChannel channel, GuildMusicManager manager) {
    if (manager.getPlayingTrack() != null && manager.isPaused()) {
      manager.resume();
      messagingService.sendChannelMessage(channel, "▶ Resumed!");
    } else {
      messagingService.sendChannelMessage(channel, "No track is paused!");
    }
  }

  private void stopAndClear(MessageChannel channel, Guild guild, GuildMusicManager manager) {
    manager.stopAndClear();
    if (guild.getAudioManager().isConnected()) {
      guild.getAudioManager().closeAudioConnection();
    }
    messagingService.sendChannelMessage(channel, "⏹ Stopped playback and cleared the queue!");
  }

  private void nowPlaying(MessageChannel channel, GuildMusicManager manager) {
    AudioTrack track = manager.getPlayingTrack();
    if (track != null) {
      String msg =
          String.format(
              "🎶 Now playing: **%s** [`%s / %s`] <%s>",
              track.getInfo().title,
              formatTime(track.getPosition()),
              formatTime(track.getDuration()),
              track.getInfo().uri);
      messagingService.sendChannelMessage(channel, msg);
    } else {
      messagingService.sendChannelMessage(channel, "Nothing is playing.");
    }
  }

  private void displayQueue(MessageChannel channel, GuildMusicManager manager) {
    if (manager.queue.isEmpty() && manager.getPlayingTrack() == null) {
      messagingService.sendChannelMessage(channel, "The queue is empty!");
      return;
    }

    StringBuilder sb = new StringBuilder();
    AudioTrack current = manager.getPlayingTrack();
    if (current != null) {
      sb.append("🎵 Now playing: ").append(current.getInfo().title).append("\n");
    }

    if (!manager.queue.isEmpty()) {
      sb.append("⏭ Up next:\n");
      int i = 1;
      for (AudioTrack track : manager.queue) {
        sb.append(i++).append(". ").append(track.getInfo().title).append("\n");
      }
    }
    messagingService.sendChannelMessage(channel, sb.toString());
  }

  private void sendSelectionMessage(
      MessageChannel channel, GuildMusicManager manager, List<AudioTrack> tracks) {
    StringBuilder sb = new StringBuilder("Select a track to play:\n");
    for (int i = 0; i < tracks.size(); i++) {
      sb.append(i + 1).append(". ").append(tracks.get(i).getInfo().title).append("\n");
    }

    List<Button> buttons = new ArrayList<>();
    for (int i = 0; i < tracks.size(); i++) {
      buttons.add(Button.primary("select_" + i, String.valueOf(i + 1)));
    }

    List<ActionRow> rows = new ArrayList<>();
    for (int i = 0; i < buttons.size(); i += 5) {
      rows.add(ActionRow.of(buttons.subList(i, Math.min(i + 5, buttons.size()))));
    }

    MessageCreateBuilder builder =
        new MessageCreateBuilder().setContent(sb.toString()).addComponents(rows);

    messagingService.sendChannelMessage(
        channel,
        builder.build(),
        message -> pendingSelections.put(message.getIdLong(), new SelectionData(manager, tracks)));
  }

  private void help(MessageChannel channel) {
    EmbedBuilder embed = new EmbedBuilder();
    embed.setTitle("🎵 Music Commands");
    embed.setColor(Color.CYAN);
    embed.setDescription("Here are the commands you can use:");

    embed.addField("!play <URL or keywords>", "Play a track or search by keywords", false);
    embed.addField("!next", "Skip the current track", false);
    embed.addField("!queue", "Show the current music queue", false);
    embed.addField("!pause", "Pause playback", false);
    embed.addField("!resume", "Resume playback", false);
    embed.addField("!stop", "Stop and clear the queue", false);
    embed.addField("!nowplaying", "Show the currently playing track", false);
    embed.addField("!help", "Show this help message", false);

    messagingService.sendChannelEmbedMessage(channel, embed.build());
  }

  private String formatTime(long millis) {
    long seconds = millis / 1000;
    long minutes = seconds / 60;
    long hours = minutes / 60;
    seconds %= 60;
    minutes %= 60;
    if (hours > 0) {
      return String.format("%d:%02d:%02d", hours, minutes, seconds);
    } else {
      return String.format("%d:%02d", minutes, seconds);
    }
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

  public record SelectionData(GuildMusicManager manager, List<AudioTrack> tracks) {

    public SelectionData(GuildMusicManager manager, List<AudioTrack> tracks) {
      this.manager = manager;
      this.tracks = List.copyOf(tracks);
    }
  }
}
