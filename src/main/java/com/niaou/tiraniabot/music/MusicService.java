package com.niaou.tiraniabot.music;

import com.niaou.tiraniabot.service.MessagingService;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MusicService {

  private static final Logger logger = LoggerFactory.getLogger(MusicService.class);

  private final AudioPlayerManager playerManager;
  private final Map<Long, GuildMusicManager> musicManagers = new ConcurrentHashMap<>();
  final Map<Long, SelectionData> pendingSelections = new ConcurrentHashMap<>();
  private final MessagingService messagingService;
  private static final String MUSIC_CHANNEL_NAME = "music";
  private static final String YOUTUBE_SOURCE_NAME = "youtube";

  public MusicService(MessagingService messagingService) {
    this.messagingService = messagingService;
    this.playerManager = new DefaultAudioPlayerManager();
    YoutubeAudioSourceManager youtubeManager = new YoutubeAudioSourceManager(true);
    playerManager.registerSourceManager(youtubeManager);
    AudioSourceManagers.registerRemoteSources(playerManager);
    AudioSourceManagers.registerLocalSource(playerManager);
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
        _ -> {
          AudioPlayer player = playerManager.createPlayer();
          var manager =
              new GuildMusicManager(player, new LinkedList<>(), channel, messagingService);
          player.addListener(
              new AudioEventAdapter() {
                @Override
                public void onTrackException(
                    AudioPlayer player, AudioTrack track, FriendlyException exception) {
                  handlePlaybackFailure(manager, track, exception);
                }
              });
          guild.getAudioManager().setSendingHandler(new MusicSendHandler(player));
          return manager;
        });
  }

  public void play(MessageReceivedEvent event, GuildMusicManager musicManager, String query) {
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

    boolean isUrl = query.startsWith("http");
    loadTracks(channel, musicManager, query, isUrl ? query : "ytsearch:" + query, !isUrl);
  }

  /**
   * Loads {@code loadQuery}. YouTube may refuse requests from the bot's IP, so when a YouTube
   * search finds nothing or fails and {@code searchSoundCloudOnFailure} is set, the same text is
   * searched on SoundCloud instead, which needs no authentication.
   */
  private void loadTracks(
      MessageChannel channel,
      GuildMusicManager musicManager,
      String query,
      String loadQuery,
      boolean searchSoundCloudOnFailure) {
    playerManager.loadItemOrdered(
        musicManager,
        loadQuery,
        new AudioLoadResultHandler() {
          @Override
          public void trackLoaded(AudioTrack track) {
            musicManager.queue(track);
            channel
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
            if (searchSoundCloudOnFailure) {
              searchSoundCloud(channel, musicManager, query);
              return;
            }
            messagingService.sendChannelMessage(channel, "❌ No results found for: " + query);
          }

          @Override
          public void loadFailed(FriendlyException exception) {
            if (searchSoundCloudOnFailure) {
              logger.warn("YouTube search failed for '{}'", query, exception);
              searchSoundCloud(channel, musicManager, query);
              return;
            }
            messagingService.sendChannelMessage(
                channel, "⚠ Failed to load track: " + exception.getMessage());
          }
        });
  }

  private void searchSoundCloud(
      MessageChannel channel, GuildMusicManager musicManager, String query) {
    loadTracks(channel, musicManager, query, "scsearch:" + query, false);
  }

  /**
   * Called when a track fails while playing, typically because YouTube refused the stream. Tries to
   * find the same song on SoundCloud so the queue keeps going instead of going silent.
   */
  private void handlePlaybackFailure(
      GuildMusicManager manager, AudioTrack failed, FriendlyException exception) {
    AudioTrackInfo info = failed.getInfo();
    logger.warn("Playback of '{}' failed: {}", info.title, exception.getMessage());

    boolean fromYoutube =
        failed.getSourceManager() != null
            && YOUTUBE_SOURCE_NAME.equals(failed.getSourceManager().getSourceName());
    if (!fromYoutube) {
      messagingService.sendChannelMessage(
          manager.channel, "⚠ Couldn't play **" + info.title + "**, skipping it.");
      return;
    }

    messagingService.sendChannelMessage(
        manager.channel,
        "⚠ YouTube wouldn't play **" + info.title + "**, trying SoundCloud instead...");
    searchSoundCloudForFallback(manager, info.title);
  }

  private void searchSoundCloudForFallback(GuildMusicManager manager, String title) {
    playerManager.loadItemOrdered(
        manager,
        "scsearch:" + title,
        new AudioLoadResultHandler() {
          @Override
          public void trackLoaded(AudioTrack track) {
            queueFallback(manager, track);
          }

          @Override
          public void playlistLoaded(AudioPlaylist playlist) {
            if (playlist.getTracks().isEmpty()) {
              noMatches();
              return;
            }
            queueFallback(manager, playlist.getTracks().getFirst());
          }

          @Override
          public void noMatches() {
            messagingService.sendChannelMessage(
                manager.channel, "❌ Couldn't find **" + title + "** on SoundCloud either.");
          }

          @Override
          public void loadFailed(FriendlyException exception) {
            messagingService.sendChannelMessage(
                manager.channel, "❌ SoundCloud lookup failed: " + exception.getMessage());
          }
        });
  }

  private void queueFallback(GuildMusicManager manager, AudioTrack track) {
    manager.queue(track);
    messagingService.sendChannelMessage(
        manager.channel,
        (manager.getPlayingTrack() == track ? "🎵 Now playing: " : "➕ Queued: ")
            + track.getInfo().title
            + " (SoundCloud)");
  }

  public void skip(MessageChannel channel, GuildMusicManager manager) {
    if (manager.queue.isEmpty()) {
      messagingService.sendChannelMessage(channel, "No track in the queue!");
    } else {
      AudioTrack next = manager.queue.peek();
      messagingService.sendChannelMessage(
          channel, "⏭ Skipped to next track \n🎵 Now playing: " + next.getInfo().title);
      manager.nextTrack();
    }
  }

  public void pause(MessageChannel channel, GuildMusicManager manager) {
    if (manager.getPlayingTrack() != null && !manager.isPaused()) {
      manager.pause();
      messagingService.sendChannelMessage(channel, "⏸ Paused!");
    } else {
      messagingService.sendChannelMessage(channel, "No track is playing or already paused!");
    }
  }

  public void resume(MessageChannel channel, GuildMusicManager manager) {
    if (manager.getPlayingTrack() != null && manager.isPaused()) {
      manager.resume();
      messagingService.sendChannelMessage(channel, "▶ Resumed!");
    } else {
      messagingService.sendChannelMessage(channel, "No track is paused!");
    }
  }

  public void stop(MessageChannel channel, Guild guild, GuildMusicManager manager) {
    manager.stopAndClear();
    if (guild.getAudioManager().isConnected()) {
      guild.getAudioManager().closeAudioConnection();
    }
    messagingService.sendChannelMessage(channel, "⏹ Stopped playback and cleared the queue!");
  }

  public void current(MessageChannel channel, GuildMusicManager manager) {
    AudioTrack track = manager.getPlayingTrack();
    if (track != null) {
      String msg = getCurrentTrackInfo(track);
      messagingService.sendChannelMessage(channel, msg);
    } else {
      messagingService.sendChannelMessage(channel, "Nothing is playing.");
    }
  }

  public void displayQueue(MessageChannel channel, GuildMusicManager manager) {
    if (manager.queue.isEmpty() && manager.getPlayingTrack() == null) {
      messagingService.sendChannelMessage(channel, "The queue is empty!");
      return;
    }

    StringBuilder sb = new StringBuilder();
    AudioTrack current = manager.getPlayingTrack();
    if (current != null) {
      sb.append(getCurrentTrackInfo(current)).append("\n");
    }

    if (!manager.queue.isEmpty()) {
      sb.append("⏭ Up next:\n");
      int i = 1;
      for (AudioTrack track : manager.queue) {
        sb.append(i++).append(". ").append(track.getInfo().title).append("\n");
      }
    }
    {
      sb.append("The queue is empty!");
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

  private String getCurrentTrackInfo(AudioTrack track) {
    return String.format(
        "🎶 Now playing: **%s** [`%s / %s`] <%s>",
        track.getInfo().title,
        formatTime(track.getPosition()),
        formatTime(track.getDuration()),
        track.getInfo().uri);
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

  public record SelectionData(GuildMusicManager manager, List<AudioTrack> tracks) {
    public SelectionData(GuildMusicManager manager, List<AudioTrack> tracks) {
      this.manager = manager;
      this.tracks = List.copyOf(tracks);
    }
  }
}
