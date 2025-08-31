package com.niaou.tiraniabot.module;

import static com.niaou.tiraniabot.module.BotModule.ADMIN;
import static com.niaou.tiraniabot.module.BotModule.GLOBAL;
import static com.niaou.tiraniabot.module.BotModule.MUSIC;

import java.util.HashMap;
import java.util.Map;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.springframework.stereotype.Service;

@Service
public class ModuleResolver {

  private final Map<String, BotModule> channelToModule = new HashMap<>();

  public ModuleResolver() {
    channelToModule.put("music", MUSIC);
    channelToModule.put("admin", ADMIN);
    channelToModule.put("general", GLOBAL);
  }

  public BotModule resolveModule(MessageChannel channel) {
    return channelToModule.getOrDefault(channel.getName().toLowerCase(), GLOBAL);
  }

  public BotModule resolveModule(MessageChannel channel, String args) {
    if (args != null && args.equalsIgnoreCase("all")) {
      return BotModule.GLOBAL;
    }
    return resolveModule(channel);
  }
}
