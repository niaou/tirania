package com.niaou.tiraniabot.command.global;

import com.niaou.tiraniabot.command.AbstractCommand;
import com.niaou.tiraniabot.service.MessagingService;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.springframework.stereotype.Service;

@Service
public class RollCommand extends AbstractCommand {

  private static final Pattern DICE_PATTERN = Pattern.compile("(\\d*)d(\\d+)");

  public RollCommand(MessagingService messagingService) {
    super(
        "!roll", "Rolls one or more dice (e.g. !roll 2d20)", 1000, List.of("!r"), messagingService);
  }

  @Override
  public void execute(MessageReceivedEvent event, String args) {
    if (!validateArgsAndNotify(event, args)) {
      return;
    }
    Matcher matcher = DICE_PATTERN.matcher(args.trim());
    matcher.matches();
    int diceCount = matcher.group(1).isEmpty() ? 1 : Integer.parseInt(matcher.group(1));
    int sides = Integer.parseInt(matcher.group(2));

    Random random = new Random();
    List<Integer> rolls = new ArrayList<>();
    int total = 0;

    for (int i = 0; i < diceCount; i++) {
      int roll = random.nextInt(sides) + 1;
      rolls.add(roll);
      total += roll;
    }

    // Find the highest roll
    int maxRoll = rolls.stream().mapToInt(Integer::intValue).max().orElse(0);

    String rollsText =
        rolls.stream()
            .map(r -> r == maxRoll ? "**" + r + "**" : String.valueOf(r))
            .collect(Collectors.joining(", "));

    StringBuilder result =
        new StringBuilder(
            "🎲 "
                + event.getMember().getNickname()
                + " rolled **"
                + diceCount
                + "d"
                + sides
                + "**:\n");
    result.append("Rolls: [").append(rollsText).append("]\n");
    if (diceCount > 1) {
      result.append("Total: **").append(total).append("**");
    }

    messagingService.sendChannelMessage(event.getChannel(), result.toString());
  }

  private boolean validateArgsAndNotify(MessageReceivedEvent event, String args) {
    boolean valid = true;

    if (args == null || args.isBlank()) {
      messagingService.sendChannelMessage(
          event.getChannel(), ("❌ Usage: `!roll XdY` (e.g. `!roll 2d6`)"));
      return false;
    }

    Matcher matcher = DICE_PATTERN.matcher(args.trim());
    if (!matcher.matches()) {
      messagingService.sendChannelMessage(
          event.getChannel(), ("❌ Invalid format! Use `XdY` (e.g. `1d20`, `3d6`)"));
      return false;
    }

    int diceCount = matcher.group(1).isEmpty() ? 1 : Integer.parseInt(matcher.group(1));
    int sides = Integer.parseInt(matcher.group(2));

    if (diceCount <= 0 || sides <= 1) {
      messagingService.sendChannelMessage(
          event.getChannel(), ("❌ Dice count must be > 0 and sides must be > 1."));
      return false;
    }

    if (diceCount > 100) {
      messagingService.sendChannelMessage(
          event.getChannel(), ("❌ Too many dice! Please roll 100 or fewer."));
      return false;
    }
    return valid;
  }
}
