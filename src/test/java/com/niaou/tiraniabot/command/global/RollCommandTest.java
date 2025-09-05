package com.niaou.tiraniabot.command.global;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.niaou.tiraniabot.service.MessagingService;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RollCommandTest {

  @Mock private MessagingService messagingService;

  @Mock private MessageChannelUnion channel;

  @Mock private Member member;

  @Mock private MessageReceivedEvent event;

  private RollCommand rollCommand;

  @BeforeEach
  void setUp() {
    when(event.getChannel()).thenReturn(channel);
    rollCommand = new RollCommand(messagingService);
  }

  @Test
  void execute_withBlankArgs_sendsUsageMessage() {
    rollCommand.execute(event, "");

    verify(messagingService)
        .sendChannelMessage(eq(channel), contains("Usage: `!roll XdY` (e.g. `!roll 2d6`)"));
  }

  @Test
  void execute_withInvalidFormat_sendsErrorMessage() {
    rollCommand.execute(event, "abc");

    verify(messagingService).sendChannelMessage(eq(channel), contains("Invalid format"));
  }

  @Test
  void execute_withZeroDice_sendsErrorMessage() {
    rollCommand.execute(event, "0d6");

    verify(messagingService).sendChannelMessage(eq(channel), contains("Dice count must be > 0"));
  }

  @Test
  void execute_withOneSidedDice_sendsErrorMessage() {
    rollCommand.execute(event, "2d1");

    verify(messagingService).sendChannelMessage(eq(channel), contains("sides must be > 1"));
  }

  @Test
  void execute_withTooManyDice_sendsErrorMessage() {
    rollCommand.execute(event, "101d6");

    verify(messagingService).sendChannelMessage(eq(channel), contains("Too many dice"));
  }

  @Test
  void execute_withValidSingleDice_rollsAndSendsMessage() {
    when(event.getMember()).thenReturn(member);

    rollCommand.execute(event, "1d6");

    verify(messagingService)
        .sendChannelMessage(
            eq(channel),
            argThat(msg -> msg.contains("rolled **1d6**") && msg.contains("Rolls: [")));
  }

  @Test
  void execute_withValidMultipleDice_rollsAndSendsMessageWithTotal() {
    when(event.getMember()).thenReturn(member);

    rollCommand.execute(event, "2d6");

    verify(messagingService)
        .sendChannelMessage(
            eq(channel),
            argThat(
                msg ->
                    msg.contains("rolled **2d6**")
                        && msg.contains("Rolls: [")
                        && msg.contains("Total: **")));
  }

  @Test
  void execute_withoutCountDefaultsToOneDie() {
    when(event.getMember()).thenReturn(member);

    rollCommand.execute(event, "d20");

    verify(messagingService)
        .sendChannelMessage(eq(channel), argThat(msg -> msg.contains("rolled **1d20**")));
  }
}
