package com.niaou.tiraniabot;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class TiraniaApplicationRunTest {

  @Test
  void run_noToken_throwsException() {
    TiraniaApplication app = new TiraniaApplication();
    assertThatThrownBy(app::run)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("TiRania token is not set");
  }

  @Test
  void run_withToken_startsJda() throws Exception {
    TiraniaApplication app = new TiraniaApplication();

    var field = TiraniaApplication.class.getDeclaredField("token");
    field.setAccessible(true);
    field.set(app, "fake-token");

    try (MockedStatic<JDABuilder> mocked = Mockito.mockStatic(JDABuilder.class)) {
      JDABuilder builderMock = mock(JDABuilder.class);
      JDA jdaMock = mock(JDA.class);

      mocked.when(() -> JDABuilder.createDefault(anyString())).thenReturn(builderMock);
      when(builderMock.build()).thenReturn(jdaMock);

      app.run();

      mocked.verify(() -> JDABuilder.createDefault("fake-token"));
      verify(builderMock).build();
    }
  }
}
