package com.niaou.tiraniabot;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.niaou.tiraniabot.music.MusicAdapter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class TiraniaApplicationTests {

  private MusicAdapter musicAdapter;

  @BeforeEach
  void setUp() {
    musicAdapter = Mockito.mock(MusicAdapter.class);
  }

  @Test
  void run_noToken_throwsException() {
    TiraniaApplication app = new TiraniaApplication(musicAdapter);
    assertThatThrownBy(app::run)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("TiRania token is not set");
  }

  @Test
  void run_withToken_startsJda() throws Exception {
    TiraniaApplication app = new TiraniaApplication(musicAdapter);

    var field = TiraniaApplication.class.getDeclaredField("token");
    field.setAccessible(true);
    field.set(app, "fake-token");

    try (MockedStatic<JDABuilder> mocked = mockStatic(JDABuilder.class)) {
      JDABuilder builderMock = mock(JDABuilder.class);
      JDA jdaMock = mock(JDA.class);

      mocked.when(() -> JDABuilder.createDefault(anyString())).thenReturn(builderMock);

      when(builderMock.enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT))
          .thenReturn(builderMock);
      when(builderMock.addEventListeners(any())).thenReturn(builderMock);
      when(builderMock.build()).thenReturn(jdaMock);

      app.run();

      mocked.verify(() -> JDABuilder.createDefault("fake-token"));
      verify(builderMock)
          .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT);
      verify(builderMock).addEventListeners(musicAdapter);
      verify(builderMock).build();
    }
  }
}
