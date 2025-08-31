package com.niaou.tiraniabot.music;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.niaou.tiraniabot.service.MessagingService;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MusicServiceTest {

  @Mock MessagingService messagingService;
  @Mock MessageChannelUnion channel;
  GuildMusicManager guildMusicManager;
  @Mock ButtonInteractionEvent buttonEvent;
  @Mock Message message;
  @Mock AudioPlayer audioPlayer;

  MusicService musicService;

  Queue<AudioTrack> q = new LinkedList<>();

  @BeforeEach
  void setUp() {
    musicService = new MusicService(messagingService);
    guildMusicManager = new GuildMusicManager(audioPlayer, q, channel, messagingService);
  }

  // --- handleButton() -------------------------

  @Test
  void handleButton_invalidComponentId_sendsError() {
    when(buttonEvent.getMessageIdLong()).thenReturn(1L);
    musicService.handleButton(buttonEvent);

    verify(messagingService, never()).editEventMessage(any(), anyString());
  }

  @Test
  void handleButton_outOfBoundsIndex_sendsError() {
    AudioTrack track = mock(AudioTrack.class);
    GuildMusicManager manager = mock(GuildMusicManager.class);
    List<AudioTrack> tracks = List.of(track);

    musicService.pendingSelections.put(42L, new MusicService.SelectionData(manager, tracks));

    when(buttonEvent.getMessageIdLong()).thenReturn(42L);
    when(buttonEvent.getComponentId()).thenReturn("select_99");

    musicService.handleButton(buttonEvent);

    verify(messagingService).replyEphemeralMessage(eq(buttonEvent), contains("out of bounds"));
  }

  @Test
  void handleButton_validSelection_queuesTrackAndEditsMessage() {
    AudioTrack track = mock(AudioTrack.class);
    AudioTrackInfo info = new AudioTrackInfo("title", "author", 1000, "id", false, "uri");
    when(track.getInfo()).thenReturn(info);

    GuildMusicManager manager = mock(GuildMusicManager.class);
    List<AudioTrack> tracks = List.of(track);

    musicService.pendingSelections.put(77L, new MusicService.SelectionData(manager, tracks));

    when(buttonEvent.getMessageIdLong()).thenReturn(77L);
    when(buttonEvent.getComponentId()).thenReturn("select_0");
    when(buttonEvent.getMessage()).thenReturn(message);
    when(message.getContentRaw()).thenReturn("original");

    musicService.handleButton(buttonEvent);

    verify(manager).queue(track);
    verify(messagingService).editEventMessage(eq(buttonEvent), contains("✅ Selected: title"));
  }

  // --- skip() -------------------------

  @Test
  void skip_emptyQueue_sendsMessage() {
    musicService.skip(channel, guildMusicManager);

    verify(messagingService).sendChannelMessage(channel, "No track in the queue!");
  }

  @Test
  void skip_withNextTrack_advancesQueue() {
    AudioTrack next = mock(AudioTrack.class);
    AudioTrackInfo info = new AudioTrackInfo("nextSong", "auth", 1000, "id", false, "uri");
    when(next.getInfo()).thenReturn(info);
    q.add(next);

    musicService.skip(channel, guildMusicManager);

    verify(messagingService).sendChannelMessage(eq(channel), contains("Now playing: nextSong"));
  }

  // --- pause() -------------------------

  @Test
  void pause_whenPlaying_pausesAndSendsMessage() {
    AudioTrack track = mock(AudioTrack.class);
    when(guildMusicManager.getPlayingTrack()).thenReturn(track);
    when(guildMusicManager.isPaused()).thenReturn(false);

    musicService.pause(channel, guildMusicManager);

    verify(messagingService).sendChannelMessage(channel, "⏸ Paused!");
  }

  @Test
  void pause_whenNoTrack_sendsError() {
    when(guildMusicManager.getPlayingTrack()).thenReturn(null);

    musicService.pause(channel, guildMusicManager);

    verify(messagingService).sendChannelMessage(channel, "No track is playing or already paused!");
  }

  // --- resume() -------------------------

  @Test
  void resume_whenPaused_resumes() {
    AudioTrack track = mock(AudioTrack.class);
    when(guildMusicManager.getPlayingTrack()).thenReturn(track);
    when(guildMusicManager.isPaused()).thenReturn(true);

    musicService.resume(channel, guildMusicManager);

    verify(messagingService).sendChannelMessage(channel, "▶ Resumed!");
  }

  @Test
  void resume_whenNothingPaused_sendsError() {
    when(guildMusicManager.getPlayingTrack()).thenReturn(null);

    musicService.resume(channel, guildMusicManager);

    verify(messagingService).sendChannelMessage(channel, "No track is paused!");
  }

  // --- current() -------------------------

  @Test
  void current_whenTrackPlaying_sendsTrackInfo() {
    AudioTrack track = mock(AudioTrack.class);
    AudioTrackInfo info = new AudioTrackInfo("song", "auth", 60000, "id", false, "http://uri");
    when(track.getInfo()).thenReturn(info);
    when(track.getPosition()).thenReturn(30000L);
    when(track.getDuration()).thenReturn(60000L);

    when(guildMusicManager.getPlayingTrack()).thenReturn(track);

    musicService.current(channel, guildMusicManager);

    verify(messagingService).sendChannelMessage(eq(channel), contains("song"));
  }

  @Test
  void current_whenNothingPlaying_sendsMessage() {
    when(guildMusicManager.getPlayingTrack()).thenReturn(null);

    musicService.current(channel, guildMusicManager);

    verify(messagingService).sendChannelMessage(channel, "Nothing is playing.");
  }

  // --- displayQueue() -------------------------

  @Test
  void displayQueue_emptyQueue_sendsEmptyMessage() {
    when(guildMusicManager.getPlayingTrack()).thenReturn(null);

    musicService.displayQueue(channel, guildMusicManager);

    verify(messagingService).sendChannelMessage(channel, "The queue is empty!");
  }

  @Test
  void displayQueue_withTracks_listsThem() {
    AudioTrack track1 = mock(AudioTrack.class);
    AudioTrackInfo info1 = new AudioTrackInfo("song1", "a", 1000, "id1", false, "uri1");
    when(track1.getInfo()).thenReturn(info1);

    when(guildMusicManager.getPlayingTrack()).thenReturn(track1);

    musicService.displayQueue(channel, guildMusicManager);

    verify(messagingService).sendChannelMessage(eq(channel), contains("song1"));
  }
}
