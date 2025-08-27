package com.niaou.tiraniabot.manager;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class GuildMusicManager {
  public final AudioPlayer player;
  public final BlockingQueue<AudioTrack> queue;

  public GuildMusicManager(AudioPlayer player) {
    this.player = player;
    this.queue = new LinkedBlockingQueue<>();
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

}
