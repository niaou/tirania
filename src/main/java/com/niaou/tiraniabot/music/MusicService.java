package com.niaou.tiraniabot.music;

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
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.springframework.stereotype.Service;

@Service
public class MusicService {

  private final AudioPlayerManager playerManager;
  private final Map<Long, GuildMusicManager> musicManagers = new ConcurrentHashMap<>();
  private final Map<Long, SelectionData> pendingSelections = new ConcurrentHashMap<>();
  private static final String MUSIC_CHANNEL_NAME = "music";

  public MusicService() {
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

    switch (command) {
      case "!play" -> {
        if (parts.length < 2) {
          channel.sendMessage("Usage: !play <URL or keywords>").queue();
          return;
        }
        playMusic(event, musicManager, parts[1]);
      }
      case "!next" -> skip(event, musicManager);
      case "!queue" -> displayQueue(event, musicManager);
      case "!pause" -> pause(event, musicManager);
      case "!resume" -> resume(event, musicManager);
      case "!stop" -> stopAndClear(event, guild, musicManager);
      case "!nowplaying" -> nowPlaying(event.getChannel(), musicManager);
      case "!help" -> help(event);
    }
  }

  public void handleButton(ButtonInteractionEvent event) {
    long messageId = event.getMessageIdLong();
    SelectionData data = pendingSelections.remove(messageId);
    if (data == null) return;

    int index;
    try {
      index = Integer.parseInt(event.getComponentId().split("_")[1]);
    } catch (NumberFormatException e) {
      event.reply("Invalid selection!").setEphemeral(true).queue();
      return;
    }

    if (index < 0 || index >= data.tracks.size()) {
      event.reply("Selection out of bounds!").setEphemeral(true).queue();
      return;
    }

    AudioTrack track = data.tracks.get(index);
    data.manager.queue(track);

    String newContent =
        event.getMessage().getContentRaw() + "\n\n✅ Selected: " + track.getInfo().title;

    event.editMessage(newContent).setComponents().queue();
  }

  public GuildMusicManager getGuildMusicManager(Guild guild, MessageChannel channel) {
    return musicManagers.computeIfAbsent(
        guild.getIdLong(),
        id -> {
          var player = playerManager.createPlayer();
          var manager = new GuildMusicManager(player, new LinkedList<>(), channel);

          AudioManager audioManager = guild.getAudioManager();
          audioManager.setSendingHandler(new MusicSendHandler(player));
          return manager;
        });
  }

  public void playMusic(MessageReceivedEvent event, GuildMusicManager musicManager, String query) {
    Guild guild = event.getGuild();
    List<VoiceChannel> channels = guild.getVoiceChannelsByName(MUSIC_CHANNEL_NAME, true);
    if (channels.isEmpty()) {
      event
          .getChannel()
          .sendMessage("No '" + MUSIC_CHANNEL_NAME + "' voice channel found!")
          .queue();
      return;
    }

    VoiceChannel channel = channels.getFirst();
    AudioManager audioManager = guild.getAudioManager();
    if (!audioManager.isConnected()) audioManager.openAudioConnection(channel);

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
              channel
                  .sendMessage(
                      "📜 Playlist queued! Total tracks added: " + playlist.getTracks().size())
                  .queue();
            } else {
              List<AudioTrack> topTracks =
                  playlist.getTracks().subList(0, Math.min(5, playlist.getTracks().size()));
              sendSelectionMessage(event, musicManager, topTracks);
            }
          }

          @Override
          public void noMatches() {
            event.getChannel().sendMessage("❌ No results found for: " + query).queue();
          }

          @Override
          public void loadFailed(FriendlyException exception) {
            event
                .getChannel()
                .sendMessage("⚠ Failed to load track: " + exception.getMessage())
                .queue();
          }
        });
  }

  private void skip(MessageReceivedEvent event, GuildMusicManager manager) {
    if (manager.queue.isEmpty()) {
      event.getChannel().sendMessage("No track in the queue!").queue();
    } else {
      AudioTrack next = manager.queue.peek();
      event
          .getChannel()
          .sendMessage("⏭ Skipped to next track \n🎵 Now playing: " + next.getInfo().title)
          .queue();
      manager.nextTrack();
    }
  }

  private void pause(MessageReceivedEvent event, GuildMusicManager manager) {
    if (manager.getPlayingTrack() != null && !manager.isPaused()) {
      manager.pause();
      event.getChannel().sendMessage("⏸ Paused!").queue();
    } else {
      event.getChannel().sendMessage("No track is playing or already paused!").queue();
    }
  }

  private void resume(MessageReceivedEvent event, GuildMusicManager manager) {
    if (manager.getPlayingTrack() != null && manager.isPaused()) {
      manager.resume();
      event.getChannel().sendMessage("▶ Resumed!").queue();
    } else {
      event.getChannel().sendMessage("No track is paused!").queue();
    }
  }

  private void stopAndClear(MessageReceivedEvent event, Guild guild, GuildMusicManager manager) {
    manager.stopAndClear();
    if (guild.getAudioManager().isConnected()) guild.getAudioManager().closeAudioConnection();
    event.getChannel().sendMessage("⏹ Stopped playback and cleared the queue!").queue();
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
      channel.sendMessage(msg).queue();
    } else {
      channel.sendMessage("Nothing is playing.").queue();
    }
  }

  private void displayQueue(MessageReceivedEvent event, GuildMusicManager manager) {
    if (manager.queue.isEmpty() && manager.getPlayingTrack() == null) {
      event.getChannel().sendMessage("The queue is empty!").queue();
      return;
    }

    StringBuilder sb = new StringBuilder();
    AudioTrack current = manager.getPlayingTrack();
    if (current != null) sb.append("🎵 Now playing: ").append(current.getInfo().title).append("\n");

    if (!manager.queue.isEmpty()) {
      sb.append("⏭ Up next:\n");
      int i = 1;
      for (AudioTrack track : manager.queue) {
        sb.append(i++).append(". ").append(track.getInfo().title).append("\n");
      }
    }
    event.getChannel().sendMessage(sb.toString()).queue();
  }

  private void sendSelectionMessage(
      MessageReceivedEvent event, GuildMusicManager manager, List<AudioTrack> tracks) {
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

    event
        .getChannel()
        .sendMessage(builder.build())
        .queue(
            message ->
                pendingSelections.put(message.getIdLong(), new SelectionData(manager, tracks)));
  }

  private void help(MessageReceivedEvent event) {
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

    event.getChannel().sendMessageEmbeds(embed.build()).queue();
  }

  private String formatTime(long millis) {
    long seconds = millis / 1000;
    long minutes = seconds / 60;
    long hours = minutes / 60;
    seconds %= 60;
    minutes %= 60;
    if (hours > 0) return String.format("%d:%02d:%02d", hours, minutes, seconds);
    else return String.format("%d:%02d", minutes, seconds);
  }

  public record SelectionData(GuildMusicManager manager, List<AudioTrack> tracks) {}
}
