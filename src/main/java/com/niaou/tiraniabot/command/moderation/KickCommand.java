package com.niaou.tiraniabot.command.moderation;

import com.niaou.tiraniabot.service.MessagingService;
import com.niaou.tiraniabot.service.ModService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.springframework.stereotype.Service;

@Service
public class KickCommand extends AbstractModCommand {

  public KickCommand(MessagingService messagingService, ModService modService) {
    super(
        "!kick",
        "Kicks one or more users from the server",
        10,
        List.of(),
        messagingService,
        modService);
  }

  @Override
  protected void executeModCommand(MessageReceivedEvent event, String args) {
    List<Member> mentionedMembers = event.getMessage().getMentions().getMembers();
    if (validateCommand(event, args, mentionedMembers)) {
      return;
    }

    List<String> kicked = new ArrayList<>();
    List<String> failed = new ArrayList<>();
    final String reason = resolveKickReason(args);

    kickMembers(
        event,
        mentionedMembers,
        reason,
        kicked,
        failed,
        () -> sendKickSummaryMessage(event, kicked, reason, failed));
  }

  private boolean validateCommand(
      MessageReceivedEvent event, String args, List<Member> mentionedMembers) {
    if (args == null || args.isBlank()) {
      messagingService.sendChannelMessage(event.getChannel(), "❌ Usage: `!kick @user`");
      return true;
    }

    if (mentionedMembers.isEmpty()) {
      messagingService.sendChannelMessage(
          event.getChannel(), "❌ Please mention at least one user to kick.");
      return true;
    }
    return false;
  }

  private void kickMembers(
      MessageReceivedEvent event,
      List<Member> mentionedMembers,
      String reason,
      List<String> kicked,
      List<String> failed,
      Runnable onComplete) {
    List<CompletableFuture<Void>> futures = new ArrayList<>();

    for (Member target : mentionedMembers) {
      CompletableFuture<Void> future = new CompletableFuture<>();
      futures.add(future);

      event
          .getGuild()
          .kick(target)
          .reason(reason)
          .queue(
              _ -> {
                kicked.add(target.getEffectiveName());
                future.complete(null);
              },
              _ -> {
                failed.add(target.getEffectiveName());
                future.complete(null);
              });
    }
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(onComplete);
  }

  private void sendKickSummaryMessage(
      MessageReceivedEvent event, List<String> kicked, String reason, List<String> failed) {

    StringBuilder result = new StringBuilder("🚨 Kick summary:\n");

    if (!kicked.isEmpty()) {
      result
          .append("✅ Kicked: ")
          .append(String.join(", ", kicked))
          .append("\nReason: ")
          .append(reason)
          .append("\n");
    }
    if (!failed.isEmpty()) {
      result.append("❌ Failed: ").append(String.join(", ", failed)).append("\n");
    }

    messagingService.sendChannelMessage(event.getChannel(), result.toString());
  }

  String resolveKickReason(String args) {
    String reason = args.replaceAll("<@!?(\\d+)>", "").trim();
    reason = reason.isBlank() ? "No reason" : reason;
    return reason;
  }
}
