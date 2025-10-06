package com.niaou.tiraniabot.command.moderation;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Answers.RETURNS_SELF;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.niaou.tiraniabot.service.MessagingService;
import com.niaou.tiraniabot.service.ModService;
import java.util.List;
import java.util.function.Consumer;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Mentions;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class KickCommandTest {

  private MessagingService messagingService;
  private KickCommand kickCommand;
  private MessageReceivedEvent event;
  private Message message;
  private Guild guild;
  private MessageChannelUnion channel;

  @BeforeEach
  void setup() {
    messagingService = mock(MessagingService.class);
    ModService modService = mock(ModService.class);
    kickCommand = new KickCommand(messagingService, modService);

    event = mock(MessageReceivedEvent.class);
    message = mock(Message.class);
    guild = mock(Guild.class);
    channel = mock(MessageChannelUnion.class);

    when(event.getMessage()).thenReturn(message);
    when(event.getGuild()).thenReturn(guild);
    when(event.getChannel()).thenReturn(channel);
  }

  @Test
  void execute_withNoArgs_sendsUsageMessage() {
    Mentions mentions = mockMentions(List.of());
    when(message.getMentions()).thenReturn(mentions);

    kickCommand.executeModCommand(event, "");

    verify(messagingService).sendChannelMessage(eq(channel), contains("Usage"));
  }

  @Test
  void execute_withNoMentionedMembers_sendsError() {
    Mentions mentions = mockMentions(List.of());
    when(message.getMentions()).thenReturn(mentions);

    kickCommand.executeModCommand(event, "some reason");

    verify(messagingService).sendChannelMessage(eq(channel), contains("Please mention"));
  }

  @Test
  void execute_withOneSuccessfulKick_sendsSummary() {
    Member target = mockMember("UserA");
    Mentions mentions = mockMentions(List.of(target));
    when(message.getMentions()).thenReturn(mentions);

    @SuppressWarnings("unchecked")
    AuditableRestAction<Void> kickAction = mock(AuditableRestAction.class, RETURNS_SELF);
    when(guild.kick(target)).thenReturn(kickAction);

    doAnswer(
            invocation -> {
              @SuppressWarnings("unchecked")
              Consumer<Void> success = invocation.getArgument(0);
              success.accept(null);
              return null;
            })
        .when(kickAction)
        .queue(any(), any());

    kickCommand.executeModCommand(event, "<@123> being rude");

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(messagingService).sendChannelMessage(eq(channel), captor.capture());

    String message = captor.getValue();
    assertThat(message)
        .contains("🚨 Kick summary")
        .contains("✅ Kicked: UserA")
        .contains("Reason: being rude");
  }

  @Test
  void execute_withFailedKick_sendsFailedSummary() {
    Member target = mockMember("UserB");
    Mentions mentions = mockMentions(List.of(target));
    when(message.getMentions()).thenReturn(mentions);

    @SuppressWarnings("unchecked")
    AuditableRestAction<Void> kickAction = mock(AuditableRestAction.class, RETURNS_SELF);
    when(guild.kick(target)).thenReturn(kickAction);

    doAnswer(
            invocation -> {
              Consumer<Throwable> failure = invocation.getArgument(1);
              failure.accept(new RuntimeException("kick failed"));
              return null;
            })
        .when(kickAction)
        .queue(any(), any());

    kickCommand.executeModCommand(event, "<@123> spam");

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(messagingService).sendChannelMessage(eq(channel), captor.capture());

    String message = captor.getValue();
    assertThat(message).contains("🚨 Kick summary").contains("❌ Failed: UserB");
  }

  @Test
  void execute_withMixedResults_reportsBothCorrectly() {
    Member successMember = mockMember("GoodUser");
    Member failMember = mockMember("BadUser");
    Mentions mentions = mockMentions(List.of(successMember, failMember));
    when(message.getMentions()).thenReturn(mentions);

    @SuppressWarnings("unchecked")
    AuditableRestAction<Void> successAction = mock(AuditableRestAction.class, RETURNS_SELF);
    @SuppressWarnings("unchecked")
    AuditableRestAction<Void> failAction = mock(AuditableRestAction.class, RETURNS_SELF);

    when(guild.kick(successMember)).thenReturn(successAction);
    when(guild.kick(failMember)).thenReturn(failAction);

    doAnswer(
            invocation -> {
              @SuppressWarnings("unchecked")
              Consumer<Void> success = invocation.getArgument(0);
              success.accept(null);
              return null;
            })
        .when(successAction)
        .queue(any(), any());

    doAnswer(
            invocation -> {
              Consumer<Throwable> failure = invocation.getArgument(1);
              failure.accept(new RuntimeException("kick failed"));
              return null;
            })
        .when(failAction)
        .queue(any(), any());

    kickCommand.executeModCommand(event, "<@1> <@2> test reason");

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(messagingService).sendChannelMessage(eq(channel), captor.capture());

    String message = captor.getValue();
    assertThat(message)
        .contains("🚨 Kick summary")
        .contains("✅ Kicked: GoodUser")
        .contains("❌ Failed: BadUser");
  }

  @Test
  void resolveKickReason_returnsTrimmedReason() {
    String input = "<@123456> being rude   ";
    String reason = kickCommand.resolveKickReason(input);
    assertEquals("being rude", reason);
  }

  @Test
  void resolveKickReason_returnsDefaultWhenEmpty() {
    String input = "<@123456>  ";
    String reason = kickCommand.resolveKickReason(input);
    assertEquals("No reason", reason);
  }

  private Mentions mockMentions(List<Member> members) {
    Mentions mentions = mock(Mentions.class);
    when(mentions.getMembers()).thenReturn(members);
    return mentions;
  }

  private Member mockMember(String name) {
    Member member = mock(Member.class);
    when(member.getEffectiveName()).thenReturn(name);
    return member;
  }
}
