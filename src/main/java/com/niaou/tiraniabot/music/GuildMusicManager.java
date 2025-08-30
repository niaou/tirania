package com.niaou.tiraniabot.music;

import com.niaou.tiraniabot.service.MessagingService;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import java.util.Queue;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

public class GuildMusicManager extends AudioEventAdapter {

  public final AudioPlayer player;
  public final Queue<AudioTrack> queue;
  public final MessageChannel channel;
  public final MessagingService messagingService;

  public GuildMusicManager(
      AudioPlayer player,
      Queue<AudioTrack> queue,
      MessageChannel channel,
      MessagingService messagingService) {
    this.player = player;
    this.messagingService = messagingService;
    this.player.addListener(this);
    this.queue = queue;
    this.channel = channel;
  }

  public void queue(AudioTrack track) {
    if (!player.startTrack(track, true)) {
      queue.offer(track);
    }
  }

  public void nextTrack() {
    player.startTrack(queue.poll(), false);
  }

  public void pause() {
    player.setPaused(true);
  }

  public void resume() {
    player.setPaused(false);
  }

  public boolean isPaused() {
    return player.isPaused();
  }

  public void stopAndClear() {
    player.stopTrack();
    queue.clear();
  }

  public AudioTrack getPlayingTrack() {
    return player.getPlayingTrack();
  }

  @Override
  public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
    if (endReason.mayStartNext) {
      AudioTrack next = queue.peek();
      if (next != null) {
        nextTrack();
        messagingService.sendChannelMessage(channel, "▶ Now playing: " + next.getInfo().title);
      }
    }
  }
}
